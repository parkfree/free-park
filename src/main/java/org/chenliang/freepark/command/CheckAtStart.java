package org.chenliang.freepark.command;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.model.Tenant;
import org.chenliang.freepark.repository.TenantRepository;
import org.chenliang.freepark.service.TaskManger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Log4j2
public class CheckAtStart implements CommandLineRunner {
  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private TaskManger taskManger;

  @Override
  public void run(String... args) throws Exception {
    List<Tenant> tenants = tenantRepository.findAll();
    for (Tenant tenant : tenants) {
      taskManger.createCheckTask(tenant);
    }
  }
}
