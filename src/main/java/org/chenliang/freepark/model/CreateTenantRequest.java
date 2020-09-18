package org.chenliang.freepark.model;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class CreateTenantRequest {
  @NotBlank
  private String carNumber;
  @NotBlank
  @Email
  private String email;
  @NotBlank
  private String owner;
  @NotBlank
  private String password;
  @NotBlank
  private String inviteCode;
}
