package org.hacker.freepark.model.rtmap;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckInRequest {
  private String openid;
  private String marketId;
  private String cardNo;
  private String mobile;
  private Integer channelId;
}
