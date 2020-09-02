package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.model.PayStatus;
import org.chenliang.freepark.model.Tenant;
import org.chenliang.freepark.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@Log4j2
public class PayTaskManager {
  private static final Duration PAY_PERIOD = Duration.ofMinutes(60);
  private static final int FIXED_PARK_TIME_MIN = 120;
  private static final int SAFE_PAY_THRESHOLD_MIN = 3;

  private final Map<Integer, ScheduledFuture<?>> payTasks = new ConcurrentHashMap<>();

  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private PaymentService paymentService;

  @Autowired
  private ThreadPoolTaskScheduler taskScheduler;

  public void schedulePayTask(Tenant tenant, Integer parkTime) {
    Duration initDelay = calculateInitPayDelay(parkTime);
    log.info("Car {} is scheduled to pay after {} min", tenant.getCarNumber(), initDelay.toMinutes());
    ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(() -> {
      pay(tenant);
    }, Instant.now().plus(initDelay), PAY_PERIOD);
    payTasks.put(tenant.getId(), future);
  }

  private void cancelPayTask(Tenant tenant) {
    boolean canceled = payTasks.get(tenant.getId()).cancel(false);
    payTasks.remove(tenant.getId());
    if (canceled) {
      log.info("Pay task for car {} is canceled", tenant.getCarNumber());
    } else {
      log.error("Cancel pay task for car {} failed", tenant.getCarNumber());
    }
  }

  private void pay(Tenant tenant) {
    PayStatus payStatus = paymentService.pay(tenant);
    if (payStatus == PayStatus.CAR_NOT_FOUND || payStatus == PayStatus.NO_AVAILABLE_MEMBER) {
      cancelPayTask(tenant);
    } else if( payStatus == PayStatus.SUCCESS) {
      if (memberRepository.findFirstByLastPaidAtBeforeAndTenant(LocalDate.now(), tenant) == null) {
        log.warn("All members for car {} are used, cancel the pay schedule task", tenant.getCarNumber());
        cancelPayTask(tenant);
      }
    }
  }

  private Duration calculateInitPayDelay(Integer parkTime) {
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
