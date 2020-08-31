package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.model.Member;
import org.chenliang.freepark.model.ParkDetail;
import org.chenliang.freepark.model.Status;
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

    ParkDetail parkDetail = getParkDetail(tenant, member);
    if (parkDetail == null) {
      return;
    }

    ParkDetail.ParkingFee parkingFee = parkDetail.getParkingFee();
    if (parkingFee.getFeeNumber() != 0) {
      // TODO: notify owner
      log.info("Need manually pay {} CMB for car {}", (parkingFee.getFeeNumber() / 100), tenant.getCarNumber());
      return;
    }

    if (parkingFee.getReceivable() == 0) {
      log.info("Car {} is already paid", tenant.getCarNumber());
      return;
    }

    if (parkingFee.getMemberDeductible() == 0) {
      log.info("This member {} doesn't has discount for today. Update this member's last paid date, and try to pay with new member", member.getMobile());
      member.setLastPaidAt(today);
      memberRepository.save(member);
      pay(tenant);
      return;
    }

    Status status;
    try {
      status = freeParkService.pay(member, parkDetail);
    } catch (Exception e) {
      log.error("Call pay API error", e);
      return;
    }

    if (status.getCode() == 401) {
      member.setLastPaidAt(today);
      memberRepository.save(member);
      log.info("Successfully paid car {} with member {}", tenant.getCarNumber(), member.getMobile());
    } else {
      log.warn("Pay car {} with member {} get error response: {}", tenant.getCarNumber(), member.getMobile(), status);
    }

    if (memberRepository.findFirstByLastPaidAtBeforeAndTenant(today, tenant) == null) {
      log.warn("No available member for car: {}, cancel the pay schedule task.", tenant.getCarNumber());
      taskManger.cancelPayTask(tenant);
    }
  }

  private ParkDetail getParkDetail(Tenant tenant, Member member) {
    ParkDetail parkDetail;
    try {
      parkDetail = freeParkService.getParkDetail(member, tenant.getCarNumber());
    } catch (Exception e) {
      log.error("Call park detail API error", e);
      return null;
    }

    if (parkDetail.getCode() == 200) {
      return parkDetail;
    } else if (parkDetail.getCode() == 400) {
      log.info("The car is exit: {}", parkDetail.getMsg());
      taskManger.cancelPayTask(tenant);
      return null;
    } else {
      log.warn("Call park detail API return unexpected error code: {}, message: {}", parkDetail.getCode(),
               parkDetail.getMsg());
      return null;
    }
  }
}
