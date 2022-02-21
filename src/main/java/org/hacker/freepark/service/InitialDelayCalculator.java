package org.hacker.freepark.service;

import java.time.Duration;
import java.time.LocalTime;

import static org.hacker.freepark.service.PayTaskManager.PAY_PERIOD;

public class InitialDelayCalculator {
  private static final Duration SAFE_PAY_THRESHOLD = Duration.ofMinutes(3);
  private static final LocalTime EXPECTED_OFF_WORK_TIME = LocalTime.of(18, 0);

  public static Duration calculateInitPayDelay(Duration currentParkDuration, LocalTime enterTime, int maxCouponCount) {
    long payPeriod = PAY_PERIOD.toHours();
    int maxCouponHours = UnitUtil.couponToHour(maxCouponCount);

    if (enterTime.isAfter(EXPECTED_OFF_WORK_TIME)) {
      return calculateInitPayDelay(currentParkDuration);
    }

    Duration parkDurBeforeOffWork = Duration.between(enterTime, EXPECTED_OFF_WORK_TIME);

    Duration parkDurBeforeFirstPay = Duration.ofHours(parkDurBeforeOffWork.toHours() / payPeriod * payPeriod);
    Duration minParkDurBeforeFirstPay = Duration.ofHours(Math.min(maxCouponHours, parkDurBeforeFirstPay.toHours()));

    if (minParkDurBeforeFirstPay.compareTo(currentParkDuration) > 0) {
      Duration initialDelay = minParkDurBeforeFirstPay.minus(currentParkDuration);
      return applySafeGuard(initialDelay);
    } else {
      return calculateInitPayDelay(currentParkDuration);
    }
  }

  private static Duration calculateInitPayDelay(Duration parkDuration) {
    Duration initialDelay = PAY_PERIOD.minusMinutes(parkDuration.toMinutes() % PAY_PERIOD.toMinutes());
    return applySafeGuard(initialDelay);
  }

  private static Duration applySafeGuard(Duration initialDelay) {
    if (initialDelay.compareTo(SAFE_PAY_THRESHOLD) > 0) {
      return initialDelay.minus(SAFE_PAY_THRESHOLD);
    }
    return Duration.ZERO;
  }
}
