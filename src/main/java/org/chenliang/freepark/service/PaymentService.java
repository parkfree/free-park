package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.PaymentResponse;
import org.chenliang.freepark.model.PaymentSearchQuery;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  @Autowired
  private EmailService emailService;

  private static final Map<PaymentStatus, String> statusComment = Map.of(
      PaymentStatus.SUCCESS, "缴费成功",
      PaymentStatus.NO_AVAILABLE_MEMBER, "没有可用的会员账号用于缴费",
      PaymentStatus.CAR_NOT_FOUND, "车辆不在停车场",
      PaymentStatus.NO_NEED_TO_PAY, "当前时段已缴费，无须再缴费",
      PaymentStatus.NEED_WECHAT_PAY, "需要通过微信手工缴费"
  );

  public PaymentResponse pay(int tenantId) {
    Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    log.info("Start to pay car {}", tenant.getCarNumber());

    Payment payment = new Payment();
    payment.setTenant(tenant);

    LocalDate today = LocalDate.now();
    Member member = memberRepository.findFirstPayableMember(today, tenant);
    if (member == null) {
      log.warn("No available member for car: {}, cancel the pay schedule task", tenant.getCarNumber());
      return createResponse(payment, PaymentStatus.NO_AVAILABLE_MEMBER);
    }

    payment.setMember(member);

    ParkDetail parkDetail;
    try {
      parkDetail = rtmapService.getParkDetail(member, tenant.getCarNumber());
    } catch (Exception e) {
      log.error("Call park detail API exception", e);
      return createResponse(payment, PaymentStatus.PARK_DETAIL_API_ERROR, "调用详情API错误");
    }

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
      log.info("Member {} doesn't has discount for today. Update its last paid date, and try to pay with new member",
               member.getMobile());
      member.setLastPaidAt(today);
      memberRepository.save(member);
      return pay(tenantId);
    }

    payment.setAmount(parkingFee.getReceivable());

    int needPoints = calculateNeedPoints(parkDetail);

    if (parkingFee.getFeeNumber() != 0 && member.getPoints() < needPoints) {
      return cannotPay(tenant, parkingFee.getFeeNumber() / 100, payment);
    }

    return makePayRequest(tenant, member, parkDetail, payment, needPoints);
  }

  private PaymentResponse cannotPay(Tenant tenant, int amount, Payment payment) {
    sendManuallyPayEmail(tenant, amount);
    log.info("Need manually pay {} RMB for car {}", amount, tenant.getCarNumber());
    return createResponse(payment, PaymentStatus.NEED_WECHAT_PAY);
  }

  private PaymentResponse makePayRequest(Tenant tenant, Member member, ParkDetail parkDetail, Payment payment, int needPoints) {
    int amount = parkDetail.getParkingFee().getFeeNumber() / 100;
    Status status;
    try {
      status = rtmapService.payWithPoints(member, parkDetail, needPoints);
    } catch (Exception e) {
      log.error("Call pay API exception", e);
      sendFailedEmail(tenant, amount);
      return createResponse(payment, PaymentStatus.PAY_API_ERROR, "调用缴费API错误");
    }

    if (status.getCode() == 401) {
      log.info("Successfully paid car {} with member {}", tenant.getCarNumber(), member.getMobile());
      updateTenantTotalAmount(tenant, payment);
      updateMemberPoints(member, needPoints);
      return createResponse(payment, PaymentStatus.SUCCESS);
    } else {
      log.warn("Paid for car {} with point failed.", tenant.getCarNumber());
      sendFailedEmail(tenant, amount);
      return createResponse(payment, PaymentStatus.PAY_API_ERROR, "调用缴费API错误");
    }
  }

  private void sendManuallyPayEmail(Tenant tenant, int amount) {
    String subject = "需要微信手工缴费";
    String content = String.format("请使用微信小程序手工缴费 %d 元。", amount);
    emailService.sendMail(tenant.getEmail(), subject, content);
  }

  private void sendFailedEmail(Tenant tenant, int amount) {
    String subject = "自动缴费失败";
    String content = String.format("Free Park 自动缴费失败，请使用微信小程序手工缴费 %d 元。", amount);
    emailService.sendMail(tenant.getEmail(), subject, content);
  }

  private void updateMemberPoints(Member member, int usedPoints) {
    if (usedPoints == 0) return;

    member.setLastPaidAt(LocalDate.now());
    member.setPoints(member.getPoints() - usedPoints);
    memberRepository.save(member);
  }

  private int calculateNeedPoints(ParkDetail parkDetail) {
    return 200 * (parkDetail.getParkingFee().getFeeNumber() / 300);
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

  public Page<Payment> getPaymentsPage(Pageable pageable, PaymentSearchQuery searchQuery) {
    return paymentRepository.findAll(searchQuery.toSpecification(), pageable);
  }
}
