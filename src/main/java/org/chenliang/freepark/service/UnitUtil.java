package org.chenliang.freepark.service;

public class UnitUtil {
  public static final int CENT_PER_HOUR = 300;
  public static final int HOUR_PER_COUPON = 2;
  public static final int POINT_PER_HOUR = 200;
  private static final int CENT_PER_YUAN = 100;

  public static int centToYuan(int cents) {
    return cents / CENT_PER_YUAN;
  }

  public static int pointToCent(int points) {
    return (points / POINT_PER_HOUR) * CENT_PER_HOUR;
  }

  public static int centToPoint(int cents) {
    return (cents / CENT_PER_HOUR) * POINT_PER_HOUR;
  }

  public static int pointToHour(int points) {
    return points / POINT_PER_HOUR;
  }

  public static int couponToHour(int coupons) {
    return coupons * HOUR_PER_COUPON;
  }

  public static int centToHour(int cents) {
    return cents / CENT_PER_HOUR;
  }
}
