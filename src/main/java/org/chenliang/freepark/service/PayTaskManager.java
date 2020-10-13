package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.model.PayTask;
import org.chenliang.freepark.model.PaymentResponse;
import org.chenliang.freepark.model.PaymentStatus;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@Log4j2
public class PayTaskManager {
  private static final Duration PAY_PERIOD = Duration.ofMinutes(60);
  private static final int SAFE_PAY_THRESHOLD_MIN = 3;

  private final Map<Integer, PayTask> payTasks = new ConcurrentHashMap<>();

  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private PaymentService paymentService;

  @Autowired
  private ThreadPoolTaskScheduler taskScheduler;

  public void schedulePayTask(Tenant tenant, Integer parkTime, LocalDateTime parkAtTime) {
    if (payTasks.get(tenant.getId()) != null) {
      log.info("The pay task for car {} is already scheduled", tenant.getCarNumber());
      return;
    }

    Duration initDelay = calculateInitPayDelay(parkTime);
    log.info("Car {} is scheduled to pay after {} min", tenant.getCarNumber(), initDelay.toMinutes());

    PayTask payTask = PayTask.builder()
        .tenantId(tenant.getId())
        .parkAt(parkAtTime)
        .createdAt(LocalDateTime.now())
        .initDelaySeconds((int) initDelay.toSeconds())
        .periodMinutes((int) PAY_PERIOD.toMinutes())
        .nextScheduledAt(LocalDateTime.now().plus(initDelay))
        .build();
    payTasks.put(tenant.getId(), payTask);

    ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(() -> {
      pay(tenant);
    }, Instant.now().plus(initDelay), PAY_PERIOD);

    payTask.setFuture(future);
  }

  public PayTask getTask(Tenant tenant) {
    return payTasks.get(tenant.getId());
  }

  public Collection<PayTask> getTasks() {
    return payTasks.values();
  }

  public void cancelPayTask(Tenant tenant) {
    boolean canceled = payTasks.get(tenant.getId()).getFuture().cancel(false);
    payTasks.remove(tenant.getId());
    if (canceled) {
      log.info("Pay task for car {} is canceled", tenant.getCarNumber());
    } else {
      log.error("Cancel pay task for car {} failed", tenant.getCarNumber());
    }
  }

  private void pay(Tenant tenant) {
    PaymentResponse paymentResponse = paymentService.pay(tenant.getId());
    updatePayTaskStatus(tenant);

    PaymentStatus paymentStatus = paymentResponse.getStatus();
    if (paymentStatus == PaymentStatus.CAR_NOT_FOUND || paymentStatus == PaymentStatus.NO_AVAILABLE_MEMBER) {
      cancelPayTask(tenant);
    } else if (paymentStatus == PaymentStatus.SUCCESS) {
      if (memberRepository.findFirstPayableMember(LocalDate.now(), tenant) == null) {
        log.warn("All members for car {} are used, cancel the pay schedule task", tenant.getCarNumber());
        cancelPayTask(tenant);
      }
    }
  }

  private void updatePayTaskStatus(Tenant tenant) {
    PayTask payTask = payTasks.get(tenant.getId());
    LocalDateTime now = LocalDateTime.now();
    payTask.setLastScheduledAt(now);
    payTask.setNextScheduledAt(now.plus(PAY_PERIOD));
  }

  private Duration calculateInitPayDelay(Integer parkTime) {
    int payPeriod = (int) PAY_PERIOD.toMinutes();

    int initialDelay = payPeriod - (parkTime % payPeriod);
    if (initialDelay > SAFE_PAY_THRESHOLD_MIN) {
      initialDelay = initialDelay - SAFE_PAY_THRESHOLD_MIN;
    }
    return Duration.ofMinutes(initialDelay);
  }
}
