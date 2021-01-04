package org.chenliang.freepark.model.rtmap;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BuyRequest {

  String portalId;
  String openId;
  String appId;
  int productId;
  String cid;
  int channelId;
  int num;

}
