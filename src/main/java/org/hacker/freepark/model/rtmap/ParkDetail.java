package org.hacker.freepark.model.rtmap;

import lombok.Data;
import lombok.experimental.Delegate;

@Data
public class ParkDetail {
  public static final Integer CAR_NOT_FOUND_CODE = 400;
  public static final Integer OK_CODE = 200;

  private Integer code;
  private String msg;
  @Delegate
  private InnerData data;

  @Data
  private static class InnerData {
    private ParkingFee parkingFee;
    private Integer everyScore;
    private Integer everyMoney;
  }

  @Data
  public static class ParkingFee {
    private Integer marketId;
    private String marketName;
    private String marketOrderNumber;
    private String carNumber;
    private Long passTime;
    private Integer parkingLongTime;
    private Long endTime;
    private Integer totalAmount;
    // receivable = parking_hours * price_per_hour (in cent)
    private Integer receivable;
    // memberDeductible = member's_free_parking_hours * price_per_hour (in cent)
    private Integer memberDeductible;
    // feeNumber = receivable - memberDeductible
    private Integer feeNumber;
  }
}
