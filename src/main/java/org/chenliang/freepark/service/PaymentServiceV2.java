package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.exception.RtmapApiErrorResponseException;
import org.chenliang.freepark.exception.RtmapApiException;
import org.chenliang.freepark.exception.RtmapApiRequestErrorException;
import org.chenliang.freepark.model.PaymentResponse;
import org.chenliang.freepark.model.PaymentSearchQuery;
import org.chenliang.freepark.model.PaymentStatus;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.entity.Payment;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.model.rtmap.CouponsResponse.Coupon;
import org.chenliang.freepark.model.rtmap.ParkDetail;
import org.chenliang.freepark.repository.MemberRepository;
import org.chenliang.freepark.repository.PaymentRepository;
import org.chenliang.freepark.repository.TenantRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.chenliang.freepark.service.PaymentUtil.CENT_PER_HOUR;
import static org.chenliang.freepark.service.PaymentUtil.centToPoint;
import static org.chenliang.freepark.service.PaymentUtil.centToYuan;

@Service
@Log4j2
public class PaymentServiceV2 {

  @Autowired
  private RtmapService rtmapService;

  @Autowired
  private PointService pointService;

  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private PaymentRepository paymentRepository;

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private ModelMapper modelMapper;

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

  public PaymentResponse pay(int tenantId) {
    Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    log.info("Start to pay car {}", tenant.getCarNumber());

    Payment payment = new Payment();
    payment.setTenant(tenant);

    Member member = memberRepository.getBestPayMember(tenant);
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
      if (e.getCode() == 400) {
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
    Coupon coupon = null;
    if (feeNumber > CENT_PER_HOUR && member.getCoupons() > 0) {
      coupon = couponsService.getOneCoupon(member);
      if (coupon != null) {
        feeNumber = feeNumber - coupon.getFacePrice();
      }
    }

    payment.setAmount(parkingFee.getReceivable());

    int needPoints = 0;
    if (feeNumber > 0) {
      needPoints = centToPoint(feeNumber);
    }
    if (member.getPoints() < needPoints) {
      return cannotPay(tenant, centToYuan(parkingFee.getFeeNumber()), payment);
    }

    return makePayRequest(tenant, member, parkDetail, payment, needPoints, coupon);
  }

  private PaymentResponse cannotPay(Tenant tenant, int amount, Payment payment) {
    sendManuallyPayEmail(tenant.getEmail(), amount);
    log.info("Need manually pay {} RMB for car {}", amount, tenant.getCarNumber());
    return createResponse(payment, PaymentStatus.NEED_WECHAT_PAY);
  }

  private PaymentResponse makePayRequest(Tenant tenant, Member member, ParkDetail parkDetail, Payment payment,
                                         int needPoints, Coupon coupon) {
    int amount = centToYuan(parkDetail.getParkingFee().getFeeNumber());
    try {
      rtmapService.payParkingFee(member, parkDetail, needPoints, coupon);
    } catch (RtmapApiException e) {
      // TODO: special handle 400 error code, this means either insufficient points, or coupon count is not correct.
      sendFailedEmail(tenant.getEmail(), amount);
      return createResponse(payment, PaymentStatus.PAY_API_ERROR);
    }

    log.info("Successfully paid car {} with member {}", tenant.getCarNumber(), member.getMobile());
    updateTenantTotalAmount(tenant, payment);
    updateMember(member, needPoints, coupon);
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

  private void updateMember(Member member, int usedPoints, Coupon coupon) {
    if (usedPoints != 0) {
      member.setPoints(member.getPoints() - usedPoints);
    }

    if (coupon != null) {
      member.setCoupons(member.getCoupons() - 1);
    }

    member.setLastPaidAt(LocalDate.now());
    memberRepository.save(member);
  }

  private void updateTenantTotalAmount(Tenant tenant, Payment payment) {
    tenant.setTotalPaidAmount(tenant.getTotalPaidAmount() + payment.getAmount());
    tenantRepository.save(tenant);
  }

  private PaymentResponse createResponse(Payment payment, PaymentStatus status) {
    Payment savedPayment = savePayment(payment, status);
    return modelMapper.map(savedPayment, PaymentResponse.class);
  }

  private Payment savePayment(Payment payment, PaymentStatus status) {
    String comment = STATUS_COMMENT_MAP.get(status);
    payment.setStatus(status);
    payment.setComment(comment);
    payment.setPaidAt(LocalDateTime.now());
    return paymentRepository.save(payment);
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

  public Page<Payment> getPaymentsPage(Pageable pageable, PaymentSearchQuery searchQuery) {
    return paymentRepository.findAll(searchQuery.toSpecification(), pageable);
  }
}
