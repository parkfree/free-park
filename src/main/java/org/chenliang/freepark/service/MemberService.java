package org.chenliang.freepark.service;

import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.MemberRequest;
import org.chenliang.freepark.model.MemberResponse;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.repository.MemberRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

@Service
public class MemberService {

  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private ModelMapper modelMapper;

  public MemberResponse createMember(MemberRequest memberRequest, Tenant tenant) {
    Member member = new Member();
    setMemberFields(memberRequest, member);
    member.setTenant(tenant);
    return modelMapper.map(memberRepository.save(member), MemberResponse.class);
  }

  public MemberResponse updateMember(Integer id, MemberRequest memberRequest, Tenant tenant) {
    Member member = memberRepository.findFirstByIdAndTenantId(id, tenant.getId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    setMemberFields(memberRequest, member);
    member.setTenant(tenant);
    return modelMapper.map(memberRepository.save(member), MemberResponse.class);
  }

  public Member getBestMemberForPayment(Tenant tenant) {
    return memberRepository.findByTenantIdAndEnablePayIsTrue(tenant.getId())
                           .stream()
                           .filter(member -> member.getCoupons() > 0 || member.getPoints() >= UnitUtil.POINT_PER_HOUR)
                           .max(Comparator.comparingInt(Member::affordableParkingHour))
                           .orElse(null);
  }

  public void updateMember(Member member, int usedPoints, int usedCoupons) {
    member.setPoints(member.getPoints() - usedPoints);
    member.setCoupons(member.getCoupons() - usedCoupons);
    member.setLastPaidAt(LocalDate.now());
    memberRepository.save(member);
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
