package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.model.Member;
import org.chenliang.freepark.model.ParkDetail;
import org.chenliang.freepark.model.Tenant;
import org.chenliang.freepark.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import static org.chenliang.freepark.service.TaskManger.PAY_PERIOD;

@Service
@Log4j2
public class CheckService {
  // 车刚入停车场的前两小时单独计费
  private static final int FIXED_PARK_TIME_MIN = 120;
  private static final int SAFE_PAY_THRESHOLD_MIN = 3;
  private static final int MAX_CHECK_COUNT = 9;

  private static final Duration CHECK_PERIOD = Duration.ofMinutes(20);
  private final Map<Integer, ScheduledFuture<?>> checkTasks = new ConcurrentHashMap<>();
  private final Map<Integer, Integer> checkCounters = new ConcurrentHashMap<>();

  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private RtmapService rtmapService;

  @Autowired
  private TaskManger taskManger;

  @Autowired
  private ThreadPoolTaskScheduler taskScheduler;

  public void scheduleCheckTask(Tenant tenant) {
    if (checkTasks.get(tenant.getId()) != null) {
      log.info("The check task for car {} is already scheduled", tenant.getCarNumber());
      return;
    }

    checkCounters.put(tenant.getId(), 0);
    ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(() -> {
      check(tenant);
    }, CHECK_PERIOD);
    checkTasks.put(tenant.getId(), future);
  }

  public void check(Tenant tenant) {
    log.info("Check if the car {} is parked, check count: {}", tenant.getCarNumber(), checkCounters.get(tenant.getId()));

    LocalDate today = LocalDate.now();
    Member member = memberRepository.findFirstByLastPaidAtBeforeAndTenant(today, tenant);
    if (member == null) {
      log.warn("No available member for car: {}, cancel the check schedule task.", tenant.getCarNumber());
      cancelCheckTask(tenant);
      return;
    }

    ParkDetail parkDetail = getParkDetail(tenant, member);
    incCheckCount(tenant);

    if (parkDetail == null) {
      if (checkCounters.get(tenant.getId()) > MAX_CHECK_COUNT) {
        log.info("Car {} reach the check count limitation: {}", tenant.getCarNumber(), MAX_CHECK_COUNT);
        cancelCheckTask(tenant);
      }
      return;
    }

    cancelCheckTask(tenant);

    Integer parkTime = parkDetail.getParkingFee().getParkingLongTime();
    Duration initDelay = getInitPayDelay(parkTime);
    taskManger.schedulePayTask(tenant, initDelay);

    LocalDateTime parkAtTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(parkDetail.getParkingFee().getPassTime()),
                                                       ZoneId.systemDefault());
    log.info("Car {} is found, it's parked at: {}, already parked: {} min, scheduled to pay after: {} min",
             tenant.getCarNumber(), parkAtTime, parkTime, initDelay.toMinutes());
  }

  public void incCheckCount(Tenant tenant) {
    checkCounters.put(tenant.getId(), checkCounters.get(tenant.getId()) + 1);
  }

  public void cancelCheckTask(Tenant tenant) {
    boolean canceled = checkTasks.get(tenant.getId()).cancel(false);
    checkTasks.remove(tenant.getId());
    checkCounters.remove(tenant.getId());
    if (canceled) {
      log.info("Check task for car {} is canceled", tenant.getCarNumber());
    } else {
      log.error("Cancel check task for car {} failed", tenant.getCarNumber());
    }
  }

  private ParkDetail getParkDetail(Tenant tenant, Member member) {
    ParkDetail parkDetail;
    try {
      parkDetail = rtmapService.getParkDetail(member, tenant.getCarNumber());
    } catch (Exception e) {
      log.error("Call park detail API error", e);
      return null;
    }

    if (parkDetail.getCode() == 200) {
      return parkDetail;
    } else if (parkDetail.getCode() == 400) {
      log.info("Car {} is not parked: {}", tenant.getCarNumber(), parkDetail.getMsg());
      return null;
    } else {
      log.warn("Call park detail API for car {} return error code: {}, message: {}",
               tenant.getCarNumber(), parkDetail.getCode(), parkDetail.getMsg());
      return null;
    }
  }

  private Duration getInitPayDelay(Integer parkTime) {
    int payPeriod = (int) PAY_PERIOD.toMinutes();

    int initialDelay;
    if (parkTime < FIXED_PARK_TIME_MIN) {
      initialDelay = FIXED_PARK_TIME_MIN + payPeriod - parkTime;
    } else {
      initialDelay = payPeriod - (parkTime % payPeriod);
    }
    if (initialDelay > SAFE_PAY_THRESHOLD_MIN) {
      initialDelay = initialDelay - SAFE_PAY_THRESHOLD_MIN;
    }
    return Duration.ofMinutes(initialDelay);
  }
}
