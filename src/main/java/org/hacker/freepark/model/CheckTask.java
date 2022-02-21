package org.hacker.freepark.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;

@Data
@Builder
public class CheckTask {
  private Integer tenantId;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createdAt;

  private Integer initDelaySeconds;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime lastScheduledAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime nextScheduledAt;

  private Integer periodMinutes;

  private Integer checkCount;

  private Integer checkCountLimit;

  @JsonIgnore
  private ScheduledFuture<?> future;
}
