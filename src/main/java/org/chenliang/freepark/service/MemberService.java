package org.chenliang.freepark.service;

import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.MemberRequest;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.repository.MemberRepository;
import org.chenliang.freepark.service.PaymentUtil.AllocateResult;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import static org.chenliang.freepark.service.UnitUtil.POINT_PER_HOUR;
import static org.chenliang.freepark.service.UnitUtil.centToHour;

@Service
public class MemberService {

  private final MemberRepository memberRepository;
  private final ModelMapper modelMapper;

  public MemberService(MemberRepository memberRepository, ModelMapper modelMapper) {
    this.memberRepository = memberRepository;
    this.modelMapper = modelMapper;
  }

  public Member createMember(MemberRequest memberRequest, Tenant tenant) {
    Member member = new Member();
    modelMapper.map(memberRequest, member);
    member.setTenant(tenant);
    return memberRepository.save(member);
  }

  public Member updateMember(Integer id, MemberRequest memberRequest, Tenant tenant) {
    Member member = memberRepository.findFirstByIdAndTenantId(id, tenant.getId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    modelMapper.map(memberRequest, member);
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

  public void decreasePointsAndCoupons(Member member, AllocateResult allocateResult) {
    member.setPoints(member.getPoints() - allocateResult.getAllocPoints());
    member.setCoupons(member.getCoupons() - allocateResult.getAllocCoupons());
    member.setLastPaidAt(LocalDate.now());
    memberRepository.save(member);
  }

  public Member getRandomPayEnabledMember(Tenant tenant) {
    return memberRepository.findFirstByEnablePayTrueAndTenant(tenant);
  }

  private BinaryOperator<Member> findBestMemberOperator(int parkingHour) {
    // if allocPoints of two member > 0 (points of two members are used)
    //   if allocPoints are different
    //     choose the member with less allocPoints
    //   else allocPoints are equal (which means allocCoupons are equal too)
    //     choose the member with less points
    // else if allocPoints of two member = 0 (points of both members are not used), (which also means allocCoupons of both are equal)
    //     choose the member with less coupons
    // else if allocPoints of one member = 0, and another > 0 (which means allocCoupons of two members are different)
    //   if the difference of two allocPoints are equal to POINTS_PER_HOUR
    //     choose the member with allocPoints > 0 (choose the one with less allocCoupons)
    //   else
    //     choose the member with allocPoints = 0 (choose the one with less allocPoints)
    return (prev, curr) -> {
      AllocateResult prevAlloc = PaymentUtil.allocate(parkingHour, prev);
      AllocateResult currAlloc = PaymentUtil.allocate(parkingHour, curr);
      if (prevAlloc.getAllocPoints() > 0 && currAlloc.getAllocPoints() > 0) {
        if (prevAlloc.getAllocPoints() != currAlloc.getAllocPoints()) {
          return MemberComparators.minByAllocPoints(prevAlloc, currAlloc);
        } else {
          return MemberComparators.minByPoints(prev, curr);
        }
      } else if (prevAlloc.getAllocPoints() == 0 && currAlloc.getAllocPoints() == 0) {
        return MemberComparators.minByCoupons(prev, curr);
      } else {
        if (Math.abs(prevAlloc.getAllocPoints() - currAlloc.getAllocPoints()) == POINT_PER_HOUR) {
          return MemberComparators.minByAllocCoupons(prevAlloc, currAlloc);
        } else {
          return MemberComparators.minByAllocPoints(prevAlloc, currAlloc);
        }
      }
    };
  }

}
