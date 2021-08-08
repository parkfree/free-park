package org.chenliang.freepark.model.rtmap;

import lombok.Data;

@Data
public class Status {
  public static final int PAY_OK_CODE = 401;
  private Integer code;
  private String msg;
}
