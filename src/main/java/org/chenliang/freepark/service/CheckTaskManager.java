package org.chenliang.freepark.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.model.CheckTask;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.model.rtmap.ParkDetail;
import org.chenliang.freepark.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class CheckTaskManager {

  private static final int MAX_CHECK_COUNT = 9;
  private static final Duration CHECK_PERIOD = Duration.ofMinutes(20);

  private final Map<Integer, CheckTask> checkTasks = new ConcurrentHashMap<>();

  @Autowired
  private MemberService memberService;

  @Autowired
  private RtmapService rtmapService;

  @Autowired
  private PayTaskManager payTaskManager;

  @Autowired
  private ThreadPoolTaskScheduler taskScheduler;

  public void scheduleCheckTask(Tenant tenant, int initDelaySeconds) {
    if (checkTasks.get(tenant.getId()) != null) {
      log.info("The check task for car {} is already scheduled", tenant.getCarNumber());
      return;
    }

    CheckTask checkTask = CheckTask.builder()
      .tenantId(tenant.getId())
      .createdAt(LocalDateTime.now())
      .initDelaySeconds(initDelaySeconds)
      .checkCount(0)
      .checkCountLimit(MAX_CHECK_COUNT)
      .nextScheduledAt(LocalDateTime.now().plusSeconds(initDelaySeconds))
      .periodMinutes((int) CHECK_PERIOD.toMinutes())
      .build();

    checkTasks.put(tenant.getId(), checkTask);

    ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(() -> check(tenant),
      Instant.now().plusSeconds(initDelaySeconds),
      CHECK_PERIOD);

    checkTask.setFuture(future);
  }

  public CheckTask getTask(Tenant tenant) {
    return checkTasks.get(tenant.getId());
  }

  public Collection<CheckTask> getTasks() {
    return checkTasks.values();
  }

  private void updateCheckTaskStatus(Tenant tenant) {
    CheckTask checkTask = checkTasks.get(tenant.getId());
    checkTask.setLastScheduledAt(LocalDateTime.now());
    checkTask.setNextScheduledAt(LocalDateTime.now().plus(CHECK_PERIOD));
    checkTask.setCheckCount(checkTask.getCheckCount() + 1);
  }

  private void check(Tenant tenant) {
    updateCheckTaskStatus(tenant);
    log.info("Check if the car {} is parked, check count: {}", tenant.getCarNumber(),
      checkTasks.get(tenant.getId()).getCheckCount());

    Member member = memberService.getRandomPayEnabledMember(tenant);
    if (member == null) {
      log.warn("No available member for car: {}, cancel the check schedule task.", tenant.getCarNumber());
      cancelCheckTask(tenant);
      return;
    }

    ParkDetail parkDetail;
    try {
      parkDetail = rtmapService.getParkDetail(member, tenant.getCarNumber());
    } catch (Exception e) {
      if (checkTasks.get(tenant.getId()).getCheckCount() == MAX_CHECK_COUNT) {
        log.info("Car {} reach the check count limitation: {}", tenant.getCarNumber(), MAX_CHECK_COUNT);
        cancelCheckTask(tenant);
      }
      return;
    }

    schedulePayTask(tenant, parkDetail);
    cancelCheckTask(tenant);
  }

  private void schedulePayTask(Tenant tenant, ParkDetail parkDetail) {
    Integer parkTime = parkDetail.getParkingFee().getParkingLongTime();
    Long parkAtTimestamp = parkDetail.getParkingFee().getPassTime();
    LocalDateTime parkAtTime = Instant.ofEpochMilli(parkAtTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
    log.info("Car {} is found, parked at: {}, already parked: {} min", tenant.getCarNumber(), parkAtTime, parkTime);

    payTaskManager.schedulePayTask(tenant, parkTime, parkAtTime);
  }

  public void cancelCheckTask(Tenant tenant) {
    boolean canceled = checkTasks.get(tenant.getId()).getFuture().cancel(false);
    checkTasks.remove(tenant.getId());
    if (canceled) {
      log.info("Check task for car {} is canceled", tenant.getCarNumber());
    } else {
      log.error("Cancel check task for car {} failed", tenant.getCarNumber());
    }
  }
}
