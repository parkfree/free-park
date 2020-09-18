package org.chenliang.freepark.model.rtmap;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Payment {
  private String wxAppId;
  private String openid;
  private String carNumber;
  private String cardName;
  private String userId;
  private String marketOrderNumber;
  private String mobile;
  private Integer receivable;
  private Integer score;
  private Integer scoreDeductible;
  private Integer scoreMinutes;
  @Builder.Default
  private String receiptVolume = "";
  private Integer receiptDeductible;
  private Integer receiptMinutes;
  private Integer memberDeductible;
  private Integer memberMinutes;
  private Integer fullDeductible;
  private Integer fullMinutes;
  private Integer feeNumber;
  private String formId;
}
