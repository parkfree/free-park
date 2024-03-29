package org.hacker.freepark.service;

import org.hacker.freepark.exception.InvalidRequestException;
import org.hacker.freepark.exception.ResourceNotFoundException;
import org.hacker.freepark.model.MemberRequest;
import org.hacker.freepark.model.entity.Member;
import org.hacker.freepark.model.entity.Tenant;
import org.hacker.freepark.model.rtmap.RtmapMember;
import org.hacker.freepark.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
public class MemberService {

  public static final int MAX_MEMBER_COUNT = 10;
  private final MemberRepository memberRepository;
  private final RtmapService rtmapService;

  public MemberService(MemberRepository memberRepository, RtmapService rtmapService) {
    this.memberRepository = memberRepository;
    this.rtmapService = rtmapService;
  }

  public Member createMember(MemberRequest memberRequest, Tenant tenant) {
    if (memberRepository.countByTenant(tenant) >= MAX_MEMBER_COUNT) {
      throw new InvalidRequestException("Reach the member count limitation: " + MAX_MEMBER_COUNT);
    }
    Member member = new Member();
    setMemberFields(memberRequest, member);
    member.setTenant(tenant);
    return memberRepository.save(member);
  }

  public Member updateMember(Integer id, MemberRequest memberRequest, Tenant tenant) {
    Member member = memberRepository.findFirstByIdAndTenantId(id, tenant.getId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    setMemberFields(memberRequest, member);
    member.setTenant(tenant);
    return memberRepository.save(member);
  }

  public Member getBestMemberForPayment(Tenant tenant, int receivableCent) {
    int parkingHour = UnitUtil.centToHour(receivableCent);
    return memberRepository.findByTenantIdAndEnablePayTrue(tenant.getId())
                           .stream()
                           .filter(member -> member.affordableParkingHour() >= parkingHour)
                           .collect(Collectors.toList()).stream()
                           .reduce(PaymentUtil.findBestMemberOperator(parkingHour))
                           .orElse(null);
  }

  public void decreasePointsAndCoupons(Member member, PaymentUtil.AllocateResult allocateResult) {
    member.setPoints(member.getPoints() - allocateResult.getAllocPoints());
    member.setCoupons(member.getCoupons() - allocateResult.getAllocCoupons());
    member.setLastPaidAt(LocalDate.now());
    memberRepository.save(member);
  }

  public Member getRandomPayEnabledMember(Tenant tenant) {
    return memberRepository.findFirstByEnablePayTrueAndTenant(tenant);
  }

  public Member getMemberWithMostCoupon(Tenant tenant) {
    return memberRepository.findFirstByEnablePayTrueAndTenantOrderByCouponsDesc(tenant);
  }

  public RtmapMember getRtmapMemberDetailByMobile(String mobile) {
    Member member = memberRepository.findFirstByEnablePayTrueOrderByIdDesc();
    return rtmapService.getMemberDetailByMobile(member, mobile)
                       .getMember();
  }

  private void setMemberFields(MemberRequest memberRequest, Member member) {
    member.setMemType(memberRequest.getMemType());
    member.setMobile(memberRequest.getMobile());
    member.setOpenId(memberRequest.getOpenId());
    member.setUserId(memberRequest.getUserId());
    member.setName(memberRequest.getName());
    member.setEnablePay(memberRequest.isEnablePay());
    member.setEnablePoint(memberRequest.isEnablePoint());
  }
}
