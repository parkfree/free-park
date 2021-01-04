package org.chenliang.freepark.model.rtmap;

import lombok.Data;

@Data
public class BuyResponse {

  int status;
  String message;
  InnerData data;

  @Data
  public class InnerData {

    String QrCode;
  }
}
