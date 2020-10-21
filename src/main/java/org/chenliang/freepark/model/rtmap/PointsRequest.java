package org.chenliang.freepark.model.rtmap;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PointsRequest {
  private int tenantType;
  private int tenantId;
  private String cid;
}
