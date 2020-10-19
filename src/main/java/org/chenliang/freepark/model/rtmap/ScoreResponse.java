package org.chenliang.freepark.model.rtmap;

import lombok.Data;
import lombok.experimental.Delegate;

@Data
public class ScoreResponse {

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
