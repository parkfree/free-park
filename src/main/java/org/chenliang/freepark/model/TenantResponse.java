package org.chenliang.freepark.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantResponse {
  private Integer id;
  private String carNumber;
  private String owner;
  private String email;
  private String role;
  private int totalPaidAmount;
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createdAt;
}
