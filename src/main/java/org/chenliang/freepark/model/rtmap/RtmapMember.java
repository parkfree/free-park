package org.chenliang.freepark.model.rtmap;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class RtmapMember {
  @JsonAlias("openid")
  private String openId;

  @JsonAlias("cid")
  private String userId;

  @JsonAlias("cardName")
  private String memType;

  private String name;
}
