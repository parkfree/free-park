package org.chenliang.freepark.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PayHistory {
  private LocalDateTime paidAt;
  private PayStatus payStatus;
}
