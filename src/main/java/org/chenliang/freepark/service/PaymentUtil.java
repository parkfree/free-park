package org.chenliang.freepark.service;

import lombok.Data;
import org.chenliang.freepark.model.entity.Member;

import static org.chenliang.freepark.service.UnitUtil.HOUR_PER_COUPON;
import static org.chenliang.freepark.service.UnitUtil.POINT_PER_HOUR;
import static org.chenliang.freepark.service.UnitUtil.couponToHour;
import static org.chenliang.freepark.service.UnitUtil.hourToPoint;

public class PaymentUtil {
  public static AllocateResult allocate(int parkingHour, Member member) {
    AllocateResult result = new AllocateResult(member);
    if (member.getPoints() < POINT_PER_HOUR) {
      result.allocCoupons = (int) Math.ceil(parkingHour / (double) HOUR_PER_COUPON);
    } else {
      result.allocCoupons = Math.min(parkingHour / HOUR_PER_COUPON, member.getCoupons());
    }

    int leftHour = parkingHour - couponToHour(result.allocCoupons);
    result.allocPoints = leftHour > 0 ? hourToPoint(leftHour) : 0;

    return result;
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
