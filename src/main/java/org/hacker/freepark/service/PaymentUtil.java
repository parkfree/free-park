package org.hacker.freepark.service;

import lombok.Data;
import org.hacker.freepark.model.entity.Member;

import java.util.function.BinaryOperator;

public class PaymentUtil {
  public static AllocateResult allocate(int parkingHour, Member member) {
    AllocateResult result = new AllocateResult(member);
    if (member.getPoints() < UnitUtil.POINT_PER_HOUR) {
      result.allocCoupons = (int) Math.ceil(parkingHour / (double) UnitUtil.HOUR_PER_COUPON);
    } else {
      result.allocCoupons = Math.min(parkingHour / UnitUtil.HOUR_PER_COUPON, member.getCoupons());
    }

    int leftHour = parkingHour - UnitUtil.couponToHour(result.allocCoupons);
    result.allocPoints = leftHour > 0 ? UnitUtil.hourToPoint(leftHour) : 0;

    return result;
  }

   public static BinaryOperator<Member> findBestMemberOperator(int parkingHour) {
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
      AllocateResult prevAlloc = allocate(parkingHour, prev);
      AllocateResult currAlloc = allocate(parkingHour, curr);
      if (prevAlloc.getAllocPoints() > 0 && currAlloc.getAllocPoints() > 0) {
        if (prevAlloc.getAllocPoints() != currAlloc.getAllocPoints()) {
          return MemberComparators.minByAllocPoints(prevAlloc, currAlloc);
        } else {
          return MemberComparators.minByPoints(prev, curr);
        }
      } else if (prevAlloc.getAllocPoints() == 0 && currAlloc.getAllocPoints() == 0) {
        return MemberComparators.minByCoupons(prev, curr);
      } else {
        if (Math.abs(prevAlloc.getAllocPoints() - currAlloc.getAllocPoints()) == UnitUtil.POINT_PER_HOUR) {
          return MemberComparators.minByAllocCoupons(prevAlloc, currAlloc);
        } else {
          return MemberComparators.minByAllocPoints(prevAlloc, currAlloc);
        }
      }
    };
  }

  @Data
  public static class AllocateResult {
    public AllocateResult(Member member) {
      this.member = member;
    }

    private Member member;
    private int allocPoints;
    private int allocCoupons;
  }
}
