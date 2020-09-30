package org.chenliang.freepark.command;

import lombok.extern.log4j.Log4j2;

import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.repository.MemberRepository;
import org.chenliang.freepark.repository.TenantRepository;
import org.chenliang.freepark.service.CheckTaskManager;
import org.chenliang.freepark.service.SignInTaskManager;
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
  private MemberRepository memberRepository;

  @Autowired
  private CheckTaskManager checkTaskManager;

  @Autowired
  private SignInTaskManager signInTaskManager;

  @Override
  public void run(String... args) throws Exception {
    scheduledCheck();
    schedulePointTask();
  }

  @Scheduled(cron = "${check-cron}")
  public void scheduledCheck() {
    List<Tenant> tenants = tenantRepository.findAll();
    for (Tenant tenant : tenants) {
      checkTaskManager.scheduleCheckTask(tenant);
    }
  }

  @Scheduled(cron = "0 0 9 * * ?")
  public void schedulePointTask(){
    final List<Member> members = memberRepository.findByEnablePointIsTrue();
    members.forEach(m -> signInTaskManager.schedulePointTask(m));
  }
}
