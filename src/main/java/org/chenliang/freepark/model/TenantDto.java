package org.chenliang.freepark.model;

import lombok.Data;

@Data
public class TenantDto {
  private Integer id;
  private String carNumber;
  private String owner;
  private String email;
}
