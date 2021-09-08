package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.model.PayTask;
import org.chenliang.freepark.model.PaymentStatus;
import org.chenliang.freepark.model.entity.Payment;
import org.chenliang.freepark.model.entity.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import static org.chenliang.freepark.service.InitialDelayCalculator.calculateInitPayDelay;
import static org.chenliang.freepark.service.UnitUtil.centToYuan;

@Service
@Log4j2
public class PayTaskManager {
  public static final Duration PAY_PERIOD = Duration.ofHours(2);

  private final Map<Integer, PayTask> payTasks = new ConcurrentHashMap<>();

  @Autowired
  private PaymentService paymentService;

  @Autowired
  private ThreadPoolTaskScheduler taskScheduler;

  public void schedulePayTask(Tenant tenant, Duration parkDuration, LocalDateTime enterTime, int maxCouponCount) {
    if (payTasks.get(tenant.getId()) != null) {
      log.info("The pay task for car {} is already scheduled", tenant.getCarNumber());
      return;
    }

    Duration initDelay = calculateInitPayDelay(parkDuration, enterTime.toLocalTime(), maxCouponCount);
    log.info("Car {} is scheduled to pay after {} min", tenant.getCarNumber(), initDelay.toMinutes());

    PayTask payTask = PayTask.builder()
                             .tenantId(tenant.getId())
                             .parkAt(enterTime)
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
    log.info("Start to pay car {}", tenant.getCarNumber());
    Payment payment = paymentService.pay(tenant);
    updatePayTaskStatus(tenant);

    PaymentStatus paymentStatus = payment.getStatus();
    if (paymentStatus == PaymentStatus.CAR_NOT_FOUND || paymentStatus == PaymentStatus.NO_AVAILABLE_MEMBER) {
      cancelPayTask(tenant);
    } else if (paymentStatus == PaymentStatus.SUCCESS) {
      log.info("Successfully pay {} RMB for car {}", centToYuan(payment.getAmount()), tenant.getCarNumber());
    }
  }

  private void updatePayTaskStatus(Tenant tenant) {
    PayTask payTask = payTasks.get(tenant.getId());
    LocalDateTime now = LocalDateTime.now();
    payTask.setLastScheduledAt(now);
    payTask.setNextScheduledAt(now.plus(PAY_PERIOD));
  }
}
