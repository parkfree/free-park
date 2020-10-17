package org.chenliang.freepark.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MemberWithTenantResponse {
  private int id;
  private Tenant tenant;
  private String userId;
  private String openId;
  private String memType;
  private String mobile;
  private String name;
  private Boolean enablePay;
  private Boolean enablePoint;
  private LocalDate lastPaidAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createdAt;

  @Data
  private static class Tenant {
    private int id;
    private String owner;
    private String carNumber;
    private String email;
  }
}
