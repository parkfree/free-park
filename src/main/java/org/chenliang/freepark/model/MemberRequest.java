package org.chenliang.freepark.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class MemberRequest {
  @NotBlank
  @Size(min = 32, max = 32)
  private String userId;

  @NotBlank
  @Size(min = 28, max = 28)
  private String openId;

  @NotBlank
  private String memType;

  @NotBlank
  @Size(min = 11, max = 11)
  private String mobile;

  @NotBlank
  @Size(max = 20)
  private String name;

  private boolean enablePay;

  private boolean enablePoint;
}
