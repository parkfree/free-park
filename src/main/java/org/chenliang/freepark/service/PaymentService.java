package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.model.PaymentResponse;
import org.chenliang.freepark.model.PaymentStatus;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.entity.Payment;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.model.rtmap.ParkDetail;
import org.chenliang.freepark.model.rtmap.Status;
import org.chenliang.freepark.repository.MemberRepository;
import org.chenliang.freepark.repository.PaymentRepository;
import org.chenliang.freepark.repository.TenantRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Log4j2
public class PaymentService {
  @Autowired
  private RtmapService rtmapService;

  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private PaymentRepository paymentRepository;

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private ModelMapper modelMapper;

  private static final Map<PaymentStatus, String> statusComment = Map.of(
      PaymentStatus.SUCCESS, "支付成功",
      PaymentStatus.NO_AVAILABLE_MEMBER, "没有可用的会员账号用于支付",
      PaymentStatus.CAR_NOT_FOUND, "车辆不在停车场",
      PaymentStatus.NO_NEED_TO_PAY, "当前时段已经支付过，不需要再支付",
      PaymentStatus.NEED_WECHAT_PAY, "需要通过微信手工支付"
  );

  public PaymentResponse pay(Tenant tenant) {
    Payment payment = new Payment();
    payment.setTenant(tenant);

    LocalDate today = LocalDate.now();
    log.info("Start to pay car {}", tenant.getCarNumber());
    Member member = memberRepository.findFirstPayableMember(today, tenant);
    if (member == null) {
      log.warn("No available member for car: {}, cancel the pay schedule task", tenant.getCarNumber());
      return createResponse(payment, PaymentStatus.NO_AVAILABLE_MEMBER);
    }

    payment.setMember(member);

    ParkDetail parkDetail = rtmapService.getParkDetail(member, tenant.getCarNumber());

    if (parkDetail.getCode() == 400) {
      log.warn("Car {} is not found when paying", tenant.getCarNumber());
      return createResponse(payment, PaymentStatus.CAR_NOT_FOUND);
    } else if (parkDetail.getCode() != 200) {
      log.warn("Call park detail API return unexpected error code: {}, message: {}", parkDetail.getCode(), parkDetail.getMsg());
      return createResponse(payment, PaymentStatus.PARK_DETAIL_API_ERROR, parkDetail.getMsg());
    }

    ParkDetail.ParkingFee parkingFee = parkDetail.getParkingFee();

    if (parkingFee.getReceivable() == 0) {
      log.info("Car {} is already paid", tenant.getCarNumber());
      return createResponse(payment, PaymentStatus.NO_NEED_TO_PAY);
    }

    if (parkingFee.getMemberDeductible() == 0) {
      log.info("Member {} doesn't has discount for today. Update its last paid date, and try to pay with new member", member.getMobile());
      member.setLastPaidAt(today);
      memberRepository.save(member);
      return pay(tenant);
    }

    payment.setAmount(parkingFee.getReceivable());

    if (parkingFee.getFeeNumber() != 0) {
      // TODO: notify owner
      log.info("Need manually pay {} CMB for car {}", (parkingFee.getFeeNumber() / 100), tenant.getCarNumber());
      return createResponse(payment, PaymentStatus.NEED_WECHAT_PAY);
    }

    Status status = rtmapService.pay(member, parkDetail);

    if (status.getCode() == 401) {
      member.setLastPaidAt(today);
      memberRepository.save(member);
      log.info("Successfully paid car {} with member {}", tenant.getCarNumber(), member.getMobile());
      updateTenantTotalAmount(tenant, payment);
      return createResponse(payment, PaymentStatus.SUCCESS);
    } else {
      log.error("Pay car {} with member {} get error response: {}", tenant.getCarNumber(), member.getMobile(), status);
      payment.setStatus(PaymentStatus.PAY_API_ERROR);
      return createResponse(payment, PaymentStatus.PAY_API_ERROR, status.getMsg());
    }
  }

  private void updateTenantTotalAmount(Tenant tenant, Payment payment) {
    tenant.setTotalPaidAmount(tenant.getTotalPaidAmount() + payment.getAmount());
    tenantRepository.save(tenant);
  }

  private PaymentResponse createResponse(Payment payment, PaymentStatus status) {
    String comment = statusComment.get(status);
    return createResponse(payment, status, comment);
  }

  private PaymentResponse createResponse(Payment payment, PaymentStatus status, String comment) {
    payment.setStatus(status);
    payment.setComment(comment);
    payment.setPaidAt(LocalDateTime.now());
    Payment savedPayment = paymentRepository.save(payment);
    return modelMapper.map(savedPayment, PaymentResponse.class);
  }

  public List<PaymentResponse> getTodayPayments(Tenant tenant) {
    LocalDate today = LocalDate.now();
    LocalDateTime from = today.atStartOfDay();
    LocalDateTime to = today.plusDays(1).atStartOfDay();
    return paymentRepository.getByTenantIdAndPaidAtBetween(tenant.getId(), from, to)
        .stream()
        .map(payment -> modelMapper.map(payment, PaymentResponse.class))
        .collect(Collectors.toList());
  }
}
