package org.chenliang.freepark.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.LocalTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class InitialDelayCalculatorTest {

  public static Stream<Arguments> provideArguments() {
    return Stream.of(
        Arguments.of(Duration.ofMinutes(10), LocalTime.of(9, 0), 5, Duration.parse("PT7H47M")),
        Arguments.of(Duration.ofMinutes(10), LocalTime.of(9, 30), 4, Duration.parse("PT7H47M")),
        Arguments.of(Duration.ofMinutes(10), LocalTime.of(9, 59), 4, Duration.parse("PT7H47M")),
        Arguments.of(Duration.ofMinutes(10), LocalTime.of(10, 0), 4, Duration.parse("PT7H47M")),
        Arguments.of(Duration.ofMinutes(10), LocalTime.of(11, 0), 4, Duration.parse("PT5H47M")),
        Arguments.of(Duration.ofMinutes(10), LocalTime.of(16, 0), 4, Duration.parse("PT1H47M")),
        Arguments.of(Duration.ofMinutes(10), LocalTime.of(16, 30), 4, Duration.parse("PT1H47M")),

        Arguments.of(Duration.ofMinutes(10), LocalTime.of(9, 0), 0, Duration.parse("PT1H47M")),
        Arguments.of(Duration.ofMinutes(10), LocalTime.of(9, 0), 3, Duration.parse("PT5H47M")),

        Arguments.of(Duration.ofMinutes(116), LocalTime.of(16, 30), 4, Duration.parse("PT1M")),
        Arguments.of(Duration.ofMinutes(117), LocalTime.of(16, 30), 4, Duration.parse("PT0M")),
        Arguments.of(Duration.ofMinutes(118), LocalTime.of(16, 30), 4, Duration.parse("PT0M")),
        Arguments.of(Duration.ofMinutes(130), LocalTime.of(16, 30), 4, Duration.parse("PT1H47M")),

        Arguments.of(Duration.ofMinutes(118), LocalTime.of(15, 0), 4, Duration.parse("PT0M")),

        Arguments.of(Duration.ofMinutes(10), LocalTime.of(18, 10), 4, Duration.parse("PT1H47M"))
    );
  }

  @ParameterizedTest
  @MethodSource("provideArguments")
  void shouldCalculateTheInitDelayCorrectly(Duration currentParkDuration, LocalTime enterTime, int maxCouponCount, Duration expectedDelay) {
    Duration duration = InitialDelayCalculator.calculateInitPayDelay(currentParkDuration, enterTime, maxCouponCount);
    assertEquals(expectedDelay, duration);
  }
}