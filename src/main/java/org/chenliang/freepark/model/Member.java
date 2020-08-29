package org.chenliang.freepark.model;

import lombok.Data;

@Data
public class Member {
  private String userId;
  private String openId;
  private String memType;
  private String mobile;
}
