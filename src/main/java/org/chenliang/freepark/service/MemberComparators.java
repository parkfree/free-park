package org.chenliang.freepark.service;

import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.service.PaymentUtil.AllocateResult;

import java.util.Comparator;

public class MemberComparators {
  public static Member minByCoupons(Member a, Member b) {
    return minBy(a, b, Comparator.comparingInt(Member::getCoupons));
  }

  public static Member minByPoints(Member a, Member b) {
    return minBy(a, b, Comparator.comparingInt(Member::getPoints));
  }

  public static Member minByAllocCoupons(AllocateResult a, AllocateResult b) {
    return minBy(a, b, Comparator.comparingInt(AllocateResult::getAllocCoupons)).getMember();
  }

  public static Member minByAllocPoints(AllocateResult a, AllocateResult b) {
    return minBy(a, b, Comparator.comparingInt(AllocateResult::getAllocPoints)).getMember();
  }

  private static <T> T minBy(T a, T b, Comparator<T> comparator) {
    return comparator.compare(a, b) <= 0 ? a : b;
  }
}
