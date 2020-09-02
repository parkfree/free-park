package org.chenliang.freepark.command;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.model.Tenant;
import org.chenliang.freepark.repository.TenantRepository;
import org.chenliang.freepark.service.CheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Log4j2
@EnableScheduling
public class CheckAtStart implements CommandLineRunner {
  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private CheckService checkService;

  @Override
  public void run(String... args) throws Exception {
    scheduledCheck();
  }

  @Scheduled(cron = "${check-cron}")
  public void scheduledCheck() {
    List<Tenant> tenants = tenantRepository.findAll();
    for (Tenant tenant : tenants) {
      checkService.scheduleCheckTask(tenant);
    }
  }
}
