package org.hacker.freepark.model.rtmap;

import lombok.Data;
import lombok.experimental.Delegate;

@Data
public class PointsResponse {
  public static final Integer OK_CODE = 200;

  private int status;
  private String message;
  @Delegate
  private InnerData data;

  @Data
  private static class InnerData {

    private int scoreF;
    private int total;
    private int balance;
    private int scoreS;
    private String name;
  }
}
