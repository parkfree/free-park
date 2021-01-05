package org.chenliang.freepark.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentResponse {
  private Integer id;
  private Integer tenantId;
  private Member member;
  private PaymentStatus status;
  private Integer amount;
  private String comment;
  private String qrCode;
  private int facePrice;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime paidAt;

  @Data
  private static class Member {
    private Integer id;
    private String mobile;
  }
}
