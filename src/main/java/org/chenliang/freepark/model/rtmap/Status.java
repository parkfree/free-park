package org.chenliang.freepark.model.rtmap;

import lombok.Data;

@Data
public class Status {
  public static final int PAY_OK_CODE = 401;
  public static final int POINT_CHECKIN_OK_CODE = 200;
  public static final int POINT_ALREADY_CHECKED_CODE = 400;
  public static final int GET_POINT_OK_CODE = 200;
  private Integer code;
  private String msg;
}
