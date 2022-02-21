package org.hacker.freepark.model.rtmap;

import lombok.Data;

@Data
public class BuyCouponResponse {
  public static final int OK_CODE = 200;

  int status;
  String message;
  InnerData data;

  @Data
  public class InnerData {

    String QrCode;
  }
}
