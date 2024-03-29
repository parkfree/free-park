package org.hacker.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.hacker.freepark.exception.RtmapApiErrorResponseException;
import org.hacker.freepark.exception.RtmapApiException;
import org.hacker.freepark.model.PaymentSearchQuery;
import org.hacker.freepark.model.PaymentStatus;
import org.hacker.freepark.model.entity.Member;
import org.hacker.freepark.model.entity.Payment;
import org.hacker.freepark.model.entity.Tenant;
import org.hacker.freepark.model.rtmap.ParkDetail;
import org.hacker.freepark.model.rtmap.ParkingCouponsResponse.Coupon;
import org.hacker.freepark.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hacker.freepark.service.UnitUtil.centToHour;

@Service
@Log4j2
public class PaymentService {
  @Autowired
  private RtmapService rtmapService;

  @Autowired
  private MemberService memberService;

  @Autowired
  private PaymentRepository paymentRepository;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private CouponsService couponsService;

  @Autowired
  private EmailService emailService;

  private static final Map<PaymentStatus, String> STATUS_COMMENT_MAP = Map.of(
      PaymentStatus.SUCCESS, "缴费成功",
      PaymentStatus.NO_AVAILABLE_MEMBER, "没有可用的会员账号用于缴费",
      PaymentStatus.CAR_NOT_FOUND, "车辆不在停车场",
      PaymentStatus.NO_NEED_TO_PAY, "当前时段已缴费，无须再缴费",
      PaymentStatus.NEED_WECHAT_PAY, "需要通过微信手工缴费",
      PaymentStatus.RTMAP_API_ERROR, "调用三方API错误"
  );

  public Page<Payment> getPaymentsPage(Pageable pageable, PaymentSearchQuery searchQuery) {
    return paymentRepository.findAll(searchQuery.toSpecification(), pageable);
  }

  public List<Payment> getTodayPayments(Tenant tenant) {
    LocalDate today = LocalDate.now();
    LocalDateTime from = today.atStartOfDay();
    LocalDateTime to = today.plusDays(1).atStartOfDay();
    return paymentRepository.getByTenantIdAndPaidAtBetween(tenant.getId(), from, to);
  }

  public Payment pay(Tenant tenant) {
    Member randomMember = memberService.getRandomPayEnabledMember(tenant);

    if (randomMember == null) {
      log.info("No enabled member for payment of car: {}", tenant.getCarNumber());
      return createPayment(tenant, null, PaymentStatus.NO_AVAILABLE_MEMBER, 0);
    }

    ParkDetail parkDetail;
    try {
      parkDetail = rtmapService.getParkDetail(randomMember, tenant.getCarNumber());
    } catch (RtmapApiException e) {
      return handleParkDetailException(tenant, randomMember, e);
    }

    Integer receivableCent = parkDetail.getParkingFee().getReceivable();

    if (receivableCent == 0) {
      log.info("Car {} is already paid", tenant.getCarNumber());
      return createPayment(tenant, randomMember, PaymentStatus.NO_NEED_TO_PAY, 0);
    }

    Member member = memberService.getBestMemberForPayment(tenant, receivableCent);
    if (member == null) {
      log.info("No available member for payment of car: {}", tenant.getCarNumber());
      return createPayment(tenant, null, PaymentStatus.NO_AVAILABLE_MEMBER, 0);
    }

    try {
      parkDetail = rtmapService.getParkDetail(member, tenant.getCarNumber());
    } catch (RtmapApiException e) {
      return handleParkDetailException(tenant, member, e);
    }

    List<Coupon> availableCoupons;
    try {
      availableCoupons = couponsService.updateAndGetCoupons(member);
    } catch (RtmapApiException e) {
      return createPayment(tenant, member, PaymentStatus.RTMAP_API_ERROR, 0);
    }

    int parkingHour = centToHour(parkDetail.getParkingFee().getFeeNumber());

    if (member.affordableParkingHour() < parkingHour) {
      sendFailedEmail(tenant.getEmail());
      log.info("Need manually pay {} parking hours for car {}", parkingHour, tenant.getCarNumber());
      return createPayment(tenant, member, PaymentStatus.NEED_WECHAT_PAY, receivableCent);
    }

    PaymentUtil.AllocateResult allocateResult = PaymentUtil.allocate(parkingHour, member);
    List<Coupon> selectedCoupons = availableCoupons.stream()
                                                   .limit(allocateResult.getAllocCoupons())
                                                   .collect(Collectors.toList());

    try {
      rtmapService.payParkingFee(member, parkDetail, allocateResult.getAllocPoints(), selectedCoupons);
    } catch (RtmapApiException e) {
      // TODO: special handle 400 error code, this means either insufficient points, or coupon count is not correct.
      sendFailedEmail(tenant.getEmail());
      return createPayment(tenant, member, PaymentStatus.RTMAP_API_ERROR, receivableCent);
    }

    Payment payment = createPayment(tenant, member, PaymentStatus.SUCCESS, receivableCent);
    tenantService.increaseTotalAmount(tenant, payment.getAmount());
    memberService.decreasePointsAndCoupons(member, allocateResult);
    return payment;
  }

  private Payment handleParkDetailException(Tenant tenant, Member member, RtmapApiException e) {
    if (e instanceof RtmapApiErrorResponseException) {
      if (((RtmapApiErrorResponseException) e).getCode() == ParkDetail.CAR_NOT_FOUND_CODE) {
        log.info("Car {} is not found when paying", tenant.getCarNumber());
        return createPayment(tenant, member, PaymentStatus.CAR_NOT_FOUND, 0);
      }
    }
    sendFailedEmail(tenant.getEmail());
    return createPayment(tenant, member, PaymentStatus.RTMAP_API_ERROR, 0);
  }

  private Payment createPayment(Tenant tenant, Member member, PaymentStatus status, int amount) {
    Payment payment = new Payment();
    payment.setTenant(tenant);
    payment.setMember(member);
    payment.setStatus(status);
    payment.setAmount(amount);
    payment.setComment(STATUS_COMMENT_MAP.get(status));
    payment.setPaidAt(LocalDateTime.now());
    return paymentRepository.save(payment);
  }

  private void sendFailedEmail(String email) {
    String subject = "自动缴费失败";
    String content = "Free Park 自动缴费失败，请使用微信小程序手工缴费。";
    emailService.sendMail(email, subject, content);
  }
}
