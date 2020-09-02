package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.model.Member;
import org.chenliang.freepark.model.ParkDetail;
import org.chenliang.freepark.model.PayStatus;
import org.chenliang.freepark.model.Status;
import org.chenliang.freepark.model.Tenant;
import org.chenliang.freepark.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Log4j2
public class PaymentService {
  @Autowired
  private RtmapService rtmapService;

  @Autowired
  private MemberRepository memberRepository;

  public PayStatus pay(Tenant tenant) {
    LocalDate today = LocalDate.now();
    log.info("Start to pay car {}", tenant.getCarNumber());
    Member member = memberRepository.findFirstByLastPaidAtBeforeAndTenant(today, tenant);
    if (member == null) {
      log.warn("No available member for car: {}, cancel the pay schedule task", tenant.getCarNumber());
      return PayStatus.NO_AVAILABLE_MEMBER;
    }

    ParkDetail parkDetail = rtmapService.getParkDetail(member, tenant.getCarNumber());

    if (parkDetail.getCode() == 400) {
      log.warn("Car {} is not found when paying", tenant.getCarNumber());
      return PayStatus.CAR_NOT_FOUND;
    } else if (parkDetail.getCode() != 200) {
      log.warn("Call park detail API return unexpected error code: {}, message: {}", parkDetail.getCode(), parkDetail.getMsg());
      return PayStatus.PARK_DETAIL_API_ERROR;
    }

    ParkDetail.ParkingFee parkingFee = parkDetail.getParkingFee();

    if (parkingFee.getReceivable() == 0) {
      log.info("Car {} is already paid", tenant.getCarNumber());
      return PayStatus.ALREADY_PAID;
    }

    if (parkingFee.getMemberDeductible() == 0) {
      log.info("Member {} doesn't has discount for today. Update its last paid date, and try to pay with new member", member.getMobile());
      member.setLastPaidAt(today);
      memberRepository.save(member);
      return pay(tenant);
    }

    if (parkingFee.getFeeNumber() != 0) {
      // TODO: notify owner
      log.info("Need manually pay {} CMB for car {}", (parkingFee.getFeeNumber() / 100), tenant.getCarNumber());
      return PayStatus.NEED_WECHAT_PAY;
    }

    Status status = rtmapService.pay(member, parkDetail);

    if (status.getCode() == 401) {
      member.setLastPaidAt(today);
      memberRepository.save(member);
      log.info("Successfully paid car {} with member {}", tenant.getCarNumber(), member.getMobile());
      return PayStatus.SUCCESS;
    } else {
      log.error("Pay car {} with member {} get error response: {}", tenant.getCarNumber(), member.getMobile(), status);
      return PayStatus.PAY_API_ERROR;
    }
  }
}
