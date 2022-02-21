package org.hacker.freepark.service;

import org.hacker.freepark.model.entity.Member;

import java.util.Comparator;

public class MemberComparators {
  public static Member minByCoupons(Member a, Member b) {
    return minBy(a, b, Comparator.comparingInt(Member::getCoupons));
  }

  public static Member minByPoints(Member a, Member b) {
    return minBy(a, b, Comparator.comparingInt(Member::getPoints));
  }

  public static Member minByAllocCoupons(PaymentUtil.AllocateResult a, PaymentUtil.AllocateResult b) {
    return minBy(a, b, Comparator.comparingInt(PaymentUtil.AllocateResult::getAllocCoupons)).getMember();
  }

  public static Member minByAllocPoints(PaymentUtil.AllocateResult a, PaymentUtil.AllocateResult b) {
    return minBy(a, b, Comparator.comparingInt(PaymentUtil.AllocateResult::getAllocPoints)).getMember();
  }

  private static <T> T minBy(T a, T b, Comparator<T> comparator) {
    return comparator.compare(a, b) <= 0 ? a : b;
  }
}
