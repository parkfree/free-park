package org.chenliang.freepark.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@Data
@Builder
public class PayTask {
  private Integer tenantId;
  private LocalDateTime parkAt;
  private LocalDateTime createAt;
  private Integer initDelay;
  private LocalDateTime lastPaidAt;
  private Integer period;
  @Builder.Default
  List<PayHistory> payHistories = new LinkedList<>();
  @JsonIgnore
  private ScheduledFuture<?> future;
}
