package org.chenliang.freepark.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenResponse {
  private String accessToken;
  private Integer expireIn;
}
