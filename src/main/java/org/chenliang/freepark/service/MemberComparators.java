package org.chenliang.freepark.service;

import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.service.PaymentUtil.AllocateResult;

import java.util.Comparator;

public class MemberComparators {
  private static final Comparator<Member> compareCoupons = Comparator.comparingInt(Member::getCoupons);
  private static final Comparator<Member> comparePoints = Comparator.comparingInt(Member::getPoints);

  private static final Comparator<AllocateResult> compareAllocCoupons = Comparator.comparingInt(AllocateResult::getAllocCoupons);
  private static final Comparator<AllocateResult> compareAllocPoints = Comparator.comparingInt(AllocateResult::getAllocPoints);

  public static Member minByCoupons(Member a, Member b) {
    return minBy(a, b, compareCoupons);
  }

  public static Member minByPoints(Member a, Member b) {
    return minBy(a, b, comparePoints);
  }

  public static Member minByAllocCoupons(AllocateResult a, AllocateResult b) {
    return minBy(a, b, compareAllocCoupons).getMember();
  }

  public static Member minByAllocPoints(AllocateResult a, AllocateResult b) {
    return minBy(a, b, compareAllocPoints).getMember();
  }

  private static <T> T minBy(T a, T b, Comparator<T> comparator) {
    return comparator.compare(a, b) <= 0 ? a : b;
  }
}
