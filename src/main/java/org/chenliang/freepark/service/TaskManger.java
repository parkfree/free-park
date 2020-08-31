package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.model.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@Log4j2
public class TaskManger {
  private static final Duration CHECK_PERIOD = Duration.ofMinutes(20);
  private final Map<Tenant, ScheduledFuture<?>> checkTasks = new ConcurrentHashMap<>();
  private final Map<Tenant, Integer> checkCounters = new ConcurrentHashMap<>();

  @Autowired
  private ThreadPoolTaskScheduler taskScheduler;

  @Autowired
  private CheckService checkService;

  public void createCheckTask(Tenant tenant) {
    ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(() -> {
      checkService.check(tenant);
    }, CHECK_PERIOD);

    checkTasks.put(tenant, future);
    checkCounters.put(tenant, 0);
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
    if (canceled) {
      log.info("Check task for tenant {} canceled successfully", tenant.getOwner());
    } else {
      log.error("Check task for tenant {} canceled failed", tenant.getOwner());
    }
  }

  public void createPayTask(Tenant tenant, Integer initialDelay) {

  }

  public void cancelPayTask(Tenant tenant) {

  }
}
