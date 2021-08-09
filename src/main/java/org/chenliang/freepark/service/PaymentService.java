package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.exception.RtmapApiErrorResponseException;
import org.chenliang.freepark.exception.RtmapApiException;
import org.chenliang.freepark.model.PaymentSearchQuery;
import org.chenliang.freepark.model.PaymentStatus;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.entity.Payment;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.model.rtmap.ParkDetail;
import org.chenliang.freepark.model.rtmap.ParkingCouponsResponse.Coupon;
import org.chenliang.freepark.repository.PaymentRepository;
import org.chenliang.freepark.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.chenliang.freepark.service.UnitUtil.*;

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
  private TenantRepository tenantRepository;

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
      PaymentStatus.PARK_DETAIL_API_ERROR, "调用详情API错误",
      PaymentStatus.PAY_API_ERROR, "调用缴费API错误",
      PaymentStatus.MEMBER_NO_DISCOUNT, "会员账号没有优惠",
      PaymentStatus.RTMAP_API_ERROR, "调用三方API错误"
  );

  public Payment pay(Tenant tenant) {
    Member member = memberService.getBestMemberForPayment(tenant);
    if (member == null) {
      log.warn("No available member for car: {}, cancel the pay schedule task", tenant.getCarNumber());
      return createPayment(tenant, null, PaymentStatus.NO_AVAILABLE_MEMBER, 0);
    }

    List<Coupon> availableCoupons;
    try {
      availableCoupons = couponsService.updateAndGetCoupons(member);
    } catch (RtmapApiException e) {
      return createPayment(tenant, member, PaymentStatus.RTMAP_API_ERROR, 0);
    }

    ParkDetail parkDetail;
    try {
      parkDetail = rtmapService.getParkDetail(member, tenant.getCarNumber());
    } catch (RtmapApiException e) {
      if (e instanceof RtmapApiErrorResponseException) {
        if( ((RtmapApiErrorResponseException) e).getCode() == ParkDetail.CAR_NOT_FOUND_CODE) {
          log.info("Car {} is not found when paying", tenant.getCarNumber());
          return createPayment(tenant, member, PaymentStatus.CAR_NOT_FOUND, 0);
        }
      }
      sendFailedEmail(tenant.getEmail());
      return createPayment(tenant, member, PaymentStatus.RTMAP_API_ERROR, 0);
    }

    Integer receivableCent = parkDetail.getParkingFee().getReceivable();
    Integer parkingFeeCent = parkDetail.getParkingFee().getFeeNumber();

    if (receivableCent == 0) {
      log.info("Car {} is already paid", tenant.getCarNumber());
      return createPayment(tenant, member, PaymentStatus.NO_NEED_TO_PAY, 0);
    }

    if (member.affordableParkingHour() < centToHour(parkingFeeCent)) {
      int yuan = centToYuan(parkingFeeCent);
      sendManuallyPayEmail(tenant.getEmail(), yuan);
      log.info("Need manually pay {} RMB for car {}", yuan, tenant.getCarNumber());
      return createPayment(tenant, member, PaymentStatus.NEED_WECHAT_PAY, receivableCent);
    }

    List<Coupon> selectedCoupons = getRequiredCoupons(parkingFeeCent, member.getPoints(), availableCoupons);
    int requiredPoints = getRequiredPoints(parkingFeeCent, selectedCoupons.size());

    try {
      rtmapService.payParkingFee(member, parkDetail, requiredPoints, selectedCoupons);
    } catch (RtmapApiException e) {
      // TODO: special handle 400 error code, this means either insufficient points, or coupon count is not correct.
      int amount = centToYuan(parkDetail.getParkingFee().getFeeNumber());
      sendFailedEmail(tenant.getEmail(), amount);
      return createPayment(tenant, member, PaymentStatus.RTMAP_API_ERROR, receivableCent);
    }

    Payment payment = createPayment(tenant, member, PaymentStatus.SUCCESS, receivableCent);
    updateTenantTotalAmount(tenant, payment);
    memberService.updateMember(member, requiredPoints, selectedCoupons.size());
    return payment;
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

  private List<Coupon> getRequiredCoupons(int parkingFeeCent, int availablePoints, List<Coupon> availableCoupons) {
    int selectCount;
    if (availablePoints < POINT_PER_HOUR) {
      selectCount = (int) Math.ceil(centToHour(parkingFeeCent) / (double) HOUR_PER_COUPON);
    } else {
      selectCount = Math.min(centToHour(parkingFeeCent) / HOUR_PER_COUPON, availableCoupons.size());
    }
    return availableCoupons.stream().limit(selectCount).collect(Collectors.toList());
  }

  private int getRequiredPoints(int parkingFeeCent, int selectCouponCount) {
    int leftHour = centToHour(parkingFeeCent) - couponToHour(selectCouponCount);
    return leftHour > 0 ? hourToPoint(leftHour) : 0;
  }

  private void sendManuallyPayEmail(String email, int amount) {
    String subject = "需要微信手工缴费";
    String content = String.format("请使用微信小程序手工缴费 %d 元。", amount);
    emailService.sendMail(email, subject, content);
  }

  private void sendFailedEmail(String email, int amount) {
    String subject = "自动缴费失败";
    String content = String.format("Free Park 自动缴费失败，请使用微信小程序手工缴费 %d 元。", amount);
    emailService.sendMail(email, subject, content);
  }

  private void sendFailedEmail(String email) {
    String subject = "自动缴费失败";
    String content = "Free Park 自动缴费失败，请使用微信小程序手工缴费。";
    emailService.sendMail(email, subject, content);
  }

  private void updateTenantTotalAmount(Tenant tenant, Payment payment) {
    tenant.setTotalPaidAmount(tenant.getTotalPaidAmount() + payment.getAmount());
    tenantRepository.save(tenant);
  }

  public Page<Payment> getPaymentsPage(Pageable pageable, PaymentSearchQuery searchQuery) {
    return paymentRepository.findAll(searchQuery.toSpecification(), pageable);
  }

  public List<Payment> getTodayPayments(Tenant tenant) {
    LocalDate today = LocalDate.now();
    LocalDateTime from = today.atStartOfDay();
    LocalDateTime to = today.plusDays(1).atStartOfDay();
    return paymentRepository.getByTenantIdAndPaidAtBetween(tenant.getId(), from, to);
  }
}
