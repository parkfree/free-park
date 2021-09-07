package org.chenliang.freepark.model.rtmap;

import lombok.Data;
import lombok.experimental.Delegate;

@Data
public class RtmapMemberResponse {
  public static final Integer OK_CODE = 200;
  private Integer status;
  private String message;

  @Delegate
  private RtmapMemberResponse.InnerData data;

  @Data
  private static class InnerData {
    private RtmapMember member;
  }
}
