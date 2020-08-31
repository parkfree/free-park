package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.model.Member;
import org.chenliang.freepark.model.ParkDetail;
import org.chenliang.freepark.model.Tenant;
import org.chenliang.freepark.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@Log4j2
public class CheckService {
  // 车刚入停车场的前两小时单独计费
  private static final int FIXED_PARK_TIME_MIN = 120;
  private static final int SAFE_PAY_THRESHOLD_MIN = 2;
  private static final int PAY_INTERVAL_MIN = 60;
  private static final int MAX_CHECK_COUNT = 9;

  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private FreeParkService freeParkService;

  @Autowired
  private TaskManger taskManger;

  public void check(Tenant tenant) {
    LocalDate today = LocalDate.now();
    Member member = memberRepository.findFirstByLastPaidAtBeforeAndTenant(today, tenant);
    if (member == null) {
      log.warn("No available member for car: {}, cancel the check schedule task.", tenant.getCarNumber());
      taskManger.cancelCheckTask(tenant);
      return;
    }

    log.info("Check if the car {} is parked, check count: {}", tenant.getCarNumber(), taskManger.getCheckCount(tenant));
    ParkDetail parkDetail = getParkDetail(tenant, member);

    if (parkDetail == null) {
      if (taskManger.getCheckCount(tenant) > MAX_CHECK_COUNT) {
        log.info("Car {} reach the check count limitation: {}", tenant.getCarNumber(), MAX_CHECK_COUNT);
        taskManger.cancelCheckTask(tenant);
      }
      return;
    }

    taskManger.cancelCheckTask(tenant);

    Integer parkTime = parkDetail.getParkingFee().getParkingLongTime();
    Integer initialDelay = getInitDelay(parkTime);
    taskManger.createPayTask(tenant, initialDelay);

    LocalDateTime parkAtTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(parkDetail.getParkingFee().getPassTime()),
                                                       ZoneId.systemDefault());
    log.info("Car {} is found, it's parked at: {}, already parked: {} min, scheduled to pay after: {} min",
             tenant.getCarNumber(), parkAtTime, parkTime, initialDelay);
  }

  private ParkDetail getParkDetail(Tenant tenant, Member member) {
    ParkDetail parkDetail;
    try {
      parkDetail = freeParkService.getParkDetail(member, tenant.getCarNumber());
    } catch (Exception e) {
      log.error("Call park detail API error", e);
      return null;
    }

    if (parkDetail.getCode() == 200) {
      return parkDetail;
    } else if (parkDetail.getCode() == 400) {
      log.info("The car is not parked: {}", parkDetail.getMsg());
      return null;
    } else {
      log.warn("Call park detail API return unexpected error code: {}, message: {}", parkDetail.getCode(),
               parkDetail.getMsg());
      return null;
    }
  }

  private Integer getInitDelay(Integer parkTime) {
    int initialDelay;
    if (parkTime < FIXED_PARK_TIME_MIN) {
      initialDelay = FIXED_PARK_TIME_MIN + PAY_INTERVAL_MIN - parkTime;
    } else {
      initialDelay = PAY_INTERVAL_MIN - (parkTime % PAY_INTERVAL_MIN);
    }
    if (initialDelay > SAFE_PAY_THRESHOLD_MIN) {
      initialDelay = initialDelay - SAFE_PAY_THRESHOLD_MIN;
    }
    return initialDelay;
  }
}
