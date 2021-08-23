package org.chenliang.freepark.service;

import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.MemberRequest;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import static org.chenliang.freepark.service.UnitUtil.centToHour;

@Service
public class MemberService {

  private final MemberRepository memberRepository;

  public MemberService(MemberRepository memberRepository) {
    this.memberRepository = memberRepository;
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
                           .reduce(findBestMemberOperator(parkingHour))
                           .orElse(null);
  }

  public void decreasePointsAndCoupons(Member member, int usedPoints, int usedCoupons) {
    member.setPoints(member.getPoints() - usedPoints);
    member.setCoupons(member.getCoupons() - usedCoupons);
    member.setLastPaidAt(LocalDate.now());
    memberRepository.save(member);
  }

  public Member getRandomPayEnabledMember(Tenant tenant) {
    return memberRepository.findFirstByEnablePayTrueAndTenant(tenant);
  }

  private BinaryOperator<Member> findBestMemberOperator(int parkingHour) {
    int needCoupon = parkingHour / UnitUtil.HOUR_PER_COUPON;
    Comparator<Member> comparator = Comparator.comparingInt(member -> Math.abs(member.getCoupons() - needCoupon));
    return (selected, curr) -> {
      if (selected.getCoupons() == curr.getCoupons() || needCoupon == 0) {
        return selected.getPoints() <= curr.getPoints() ? selected : curr;
      }

      if (selected.getCoupons() >= needCoupon && curr.getCoupons() < needCoupon) {
        return selected;
      } else if (selected.getCoupons() < needCoupon && curr.getCoupons() >= needCoupon) {
        return curr;
      } else {
        return comparator.compare(selected, curr) <= 0 ? selected : curr;
      }
    };
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
