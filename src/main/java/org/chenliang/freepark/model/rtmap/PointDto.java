package org.chenliang.freepark.model.rtmap;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PointDto {
  private String openid;
  private String marketId;
  private String cardNo;
  private String mobile;
  private Integer channelId;

}
