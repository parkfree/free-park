package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.model.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@Log4j2
public class TaskManger {
  public static final Duration PAY_PERIOD = Duration.ofMinutes(60);
  private static final Duration CHECK_PERIOD = Duration.ofMinutes(20);
  private final Map<Tenant, ScheduledFuture<?>> checkTasks = new ConcurrentHashMap<>();
  private final Map<Tenant, Integer> checkCounters = new ConcurrentHashMap<>();
  private final Map<Tenant, ScheduledFuture<?>> payTasks = new ConcurrentHashMap<>();

  @Autowired
  private ThreadPoolTaskScheduler taskScheduler;

  @Autowired
  private CheckService checkService;

  @Autowired
  private PayService payService;

  public void scheduleCheckTask(Tenant tenant) {
    checkCounters.put(tenant, 0);
    ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(() -> {
      checkService.check(tenant);
    }, CHECK_PERIOD);

    checkTasks.put(tenant, future);
  }

  public void incCheckCount(Tenant tenant) {
    checkCounters.put(tenant, checkCounters.get(tenant) + 1);
  }

  public Integer getCheckCount(Tenant tenant) {
    return checkCounters.get(tenant);
  }

  public void cancelCheckTask(Tenant tenant) {
    boolean canceled = checkTasks.get(tenant).cancel(false);
    checkTasks.remove(tenant);
    checkCounters.remove(tenant);
    if (canceled) {
      log.info("Check task for tenant {} canceled successfully", tenant.getCarNumber());
    } else {
      log.error("Check task for tenant {} canceled failed", tenant.getCarNumber());
    }
  }

  public void schedulePayTask(Tenant tenant, Duration initialDelay) {
    ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(() -> {
      payService.pay(tenant);
    }, Instant.now().plus(initialDelay), PAY_PERIOD);
    payTasks.put(tenant, future);
  }

  public void cancelPayTask(Tenant tenant) {
    boolean canceled = payTasks.get(tenant).cancel(false);
    payTasks.remove(tenant);
    if (canceled) {
      log.info("Pay task for tenant {} canceled successfully", tenant.getCarNumber());
    } else {
      log.error("Pay task for tenant {} canceled failed", tenant.getCarNumber());
    }
  }
}
