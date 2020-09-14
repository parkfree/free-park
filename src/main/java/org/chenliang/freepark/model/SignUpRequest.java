package org.chenliang.freepark.model;

import lombok.Data;

@Data
public class SignUpRequest {
  private String carNumber;
  private String email;
  private String owner;
  private String password;
  private String inviteCode;
}
