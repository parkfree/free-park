package org.chenliang.freepark.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentDetailResponse {
  private Integer id;
  private Tenant tenant;
  private Member member;
  private PaymentStatus status;
  private Integer amount;
  private String comment;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime paidAt;

  @Data
  private static class Member {
    private Integer id;
    private String mobile;
    private String name;
  }

  @Data
  private static class Tenant {
    private int id;
    private String owner;
    private String carNumber;
    private String email;
  }
}
