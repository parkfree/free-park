package org.chenliang.freepark.model.rtmap;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Builder
public class CheckInRequest {
  private String openid;
  private String marketId;
  private String cardNo;
  private String mobile;
  private Integer channelId;
}
