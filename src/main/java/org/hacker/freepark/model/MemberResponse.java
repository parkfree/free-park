package org.hacker.freepark.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MemberResponse {
  private Integer id;
  private Integer tenantId;
  private String userId;
  private String openId;
  private String memType;
  private String mobile;
  private String name;
  private Integer points;
  private Boolean enablePay;
  private Boolean enablePoint;
  private LocalDate lastPaidAt;
  private Integer coupons;
}
