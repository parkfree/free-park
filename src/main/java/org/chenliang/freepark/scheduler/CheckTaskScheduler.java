package org.chenliang.freepark.scheduler;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.repository.TenantRepository;
import org.chenliang.freepark.service.CheckTaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@Log4j2
public class CheckTaskScheduler implements CommandLineRunner {
  public static final int MAX_INIT_DELAY_SECONDS = 1200; // within 20 minutes

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private CheckTaskManager checkTaskManager;

  @Override
  public void run(String... args) throws Exception {
    scheduledCheck();
  }

  @Scheduled(cron = "${check-cron}")
  public void scheduledCheck() {
    List<Tenant> tenants = tenantRepository.findAll();
    Random random = new Random();
    for (Tenant tenant : tenants) {
      int initDelaySeconds = random.nextInt(MAX_INIT_DELAY_SECONDS);
      checkTaskManager.scheduleCheckTask(tenant, initDelaySeconds);
    }
  }
}
