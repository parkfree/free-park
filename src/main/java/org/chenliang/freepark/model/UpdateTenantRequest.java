package org.chenliang.freepark.model;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class UpdateTenantRequest {
  @NotBlank
  private String carNumber;

  @NotBlank
  @Email
  private String email;

  @NotBlank
  private String owner;

  private String password;
  @Pattern(regexp = "ROLE_(AMDMIN|USER)")
  private String role;
}
