package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.model.Member;
import org.chenliang.freepark.model.Tenant;
import org.chenliang.freepark.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Log4j2
public class PayService {
  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private FreeParkService freeParkService;

  @Autowired
  private TaskManger taskManger;

  public void pay(Tenant tenant) {
    LocalDate today = LocalDate.now();
    Member member = memberRepository.findFirstByLastPaidAtBeforeAndTenant(today, tenant);
    if (member == null) {
      log.warn("No available member for car: {}, cancel the pay schedule task.", tenant.getCarNumber());
      taskManger.cancelPayTask(tenant);
      return;
    }
  }
}
