package org.chenliang.freepark.service;

import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.MemberRequest;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.model.rtmap.RtmapMember;
import org.chenliang.freepark.repository.MemberRepository;
import org.chenliang.freepark.service.PaymentUtil.AllocateResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.stream.Collectors;

import static org.chenliang.freepark.service.UnitUtil.centToHour;

@Service
public class MemberService {

  private final MemberRepository memberRepository;
  private RtmapService rtmapService;

  public MemberService(MemberRepository memberRepository, RtmapService rtmapService) {
    this.memberRepository = memberRepository;
    this.rtmapService = rtmapService;
  }

  public Member createMember(MemberRequest memberRequest, Tenant tenant) {
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
    int parkingHour = centToHour(receivableCent);
    return memberRepository.findByTenantIdAndEnablePayTrue(tenant.getId())
                           .stream()
                           .filter(member -> member.affordableParkingHour() >= parkingHour)
                           .collect(Collectors.toList()).stream()
                           .reduce(PaymentUtil.findBestMemberOperator(parkingHour))
                           .orElse(null);
  }

  public void decreasePointsAndCoupons(Member member, AllocateResult allocateResult) {
    member.setPoints(member.getPoints() - allocateResult.getAllocPoints());
    member.setCoupons(member.getCoupons() - allocateResult.getAllocCoupons());
    member.setLastPaidAt(LocalDate.now());
    memberRepository.save(member);
  }

  public Member getRandomPayEnabledMember(Tenant tenant) {
    return memberRepository.findFirstByEnablePayTrueAndTenant(tenant);
  }

  public RtmapMember getRtmapMemberDetailByMobile(String mobile) {
    Member member = memberRepository.findFirstByEnablePayTrue();
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
