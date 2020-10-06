package org.chenliang.freepark.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;

@Data
@Builder
public class PayTask {
  private Integer tenantId;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime parkAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createdAt;

  private Integer initDelayMinutes;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime lastScheduledAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime nextScheduledAt;

  private Integer periodMinutes;

  @JsonIgnore
  private ScheduledFuture<?> future;
}
