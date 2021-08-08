package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.exception.RtmapApiErrorResponseException;
import org.chenliang.freepark.exception.RtmapApiException;
import org.chenliang.freepark.exception.RtmapApiRequestErrorException;
import org.chenliang.freepark.model.PaymentSearchQuery;
import org.chenliang.freepark.model.PaymentStatus;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.entity.Payment;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.model.rtmap.ParkDetail;
import org.chenliang.freepark.model.rtmap.ParkingCouponsResponse.Coupon;
import org.chenliang.freepark.repository.MemberRepository;
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

import static org.chenliang.freepark.service.UnitUtil.HOUR_PER_COUPON;
import static org.chenliang.freepark.service.UnitUtil.POINT_PER_HOUR;
import static org.chenliang.freepark.service.UnitUtil.centToHour;
import static org.chenliang.freepark.service.UnitUtil.centToYuan;

@Service
@Log4j2
public class PaymentService {
  @Autowired
  private RtmapService rtmapService;

  @Autowired
  private MemberRepository memberRepository;

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
      PaymentStatus.MEMBER_NO_DISCOUNT, "会员账号没有优惠"
  );

  public Payment pay(Tenant tenant) {
    log.info("Start to pay car {}", tenant.getCarNumber());

    Payment payment = new Payment();
    payment.setTenant(tenant);

    Member member = memberService.getBestMemberForPayment(tenant);
    if (member == null) {
      log.warn("No available member for car: {}, cancel the pay schedule task", tenant.getCarNumber());
      return createResponse(payment, PaymentStatus.NO_AVAILABLE_MEMBER);
    }

    payment.setMember(member);

    ParkDetail parkDetail;
    try {
      parkDetail = rtmapService.getParkDetail(member, tenant.getCarNumber());
    } catch (RtmapApiRequestErrorException e) {
      sendFailedEmail(tenant.getEmail());
      return createResponse(payment, PaymentStatus.PARK_DETAIL_API_ERROR);
    } catch (RtmapApiErrorResponseException e) {
      if (e.getCode() == ParkDetail.CAR_NOT_FOUND_CODE) {
        log.warn("Car {} is not found when paying", tenant.getCarNumber());
        return createResponse(payment, PaymentStatus.CAR_NOT_FOUND);
      } else {
        sendFailedEmail(tenant.getEmail());
        return createResponse(payment, PaymentStatus.PARK_DETAIL_API_ERROR);
      }
    }

    ParkDetail.ParkingFee parkingFee = parkDetail.getParkingFee();
    if (parkingFee.getReceivable() == 0) {
      log.info("Car {} is already paid", tenant.getCarNumber());
      return createResponse(payment, PaymentStatus.NO_NEED_TO_PAY);
    }

    int feeNumber = parkDetail.getParkingFee().getFeeNumber();
    payment.setAmount(feeNumber);

    List<Coupon> coupons = couponsService.updateAndGetCoupons(member);

    if (member.affordableParkingHour() < centToHour(feeNumber)) {
      return cannotPay(tenant, centToYuan(parkingFee.getFeeNumber()), payment);
    }

    int requiredCouponCount;
    int requiredPoints;
    if (member.getPoints() < POINT_PER_HOUR) {
      requiredCouponCount = (int) Math.ceil(centToHour(feeNumber) / (double) HOUR_PER_COUPON);
      requiredPoints = 0;
    } else {
      requiredCouponCount = Math.min(centToHour(feeNumber) / HOUR_PER_COUPON, coupons.size());
      requiredPoints = (centToHour(feeNumber) - requiredCouponCount * HOUR_PER_COUPON) * POINT_PER_HOUR;
    }

    List<Coupon> selectedCoupons = coupons.stream().limit(requiredCouponCount).collect(Collectors.toList());

    return makePayRequest(tenant, member, parkDetail, payment, requiredPoints, selectedCoupons);
  }

  private Payment cannotPay(Tenant tenant, int amount, Payment payment) {
    sendManuallyPayEmail(tenant.getEmail(), amount);
    log.info("Need manually pay {} RMB for car {}", amount, tenant.getCarNumber());
    return createResponse(payment, PaymentStatus.NEED_WECHAT_PAY);
  }

  private Payment makePayRequest(Tenant tenant, Member member, ParkDetail parkDetail, Payment payment,
                                 int requiredPoints, List<Coupon> coupons) {
    try {
      rtmapService.payParkingFee(member, parkDetail, requiredPoints, coupons);
    } catch (RtmapApiException e) {
      // TODO: special handle 400 error code, this means either insufficient points, or coupon count is not correct.
      int amount = centToYuan(parkDetail.getParkingFee().getFeeNumber());
      sendFailedEmail(tenant.getEmail(), amount);
      return createResponse(payment, PaymentStatus.PAY_API_ERROR);
    }

    log.info("Successfully paid car {} with member {}", tenant.getCarNumber(), member.getMobile());
    updateTenantTotalAmount(tenant, payment);
    updateMember(member, requiredPoints, coupons.size());
    return createResponse(payment, PaymentStatus.SUCCESS);
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

  private void updateMember(Member member, int usedPoints, int usedCoupons) {
    member.setPoints(member.getPoints() - usedPoints);
    member.setCoupons(member.getCoupons() - usedCoupons);
    member.setLastPaidAt(LocalDate.now());
    memberRepository.save(member);
  }

  private void updateTenantTotalAmount(Tenant tenant, Payment payment) {
    tenant.setTotalPaidAmount(tenant.getTotalPaidAmount() + payment.getAmount());
    tenantRepository.save(tenant);
  }

  private Payment createResponse(Payment payment, PaymentStatus status) {
    String comment = STATUS_COMMENT_MAP.get(status);
    payment.setStatus(status);
    payment.setComment(comment);
    payment.setPaidAt(LocalDateTime.now());
    return paymentRepository.save(payment);
  }

  public Page<Payment> getPaymentsPage(Pageable pageable, PaymentSearchQuery searchQuery) {
    return paymentRepository.findAll(searchQuery.toSpecification(), pageable);
  }
}
