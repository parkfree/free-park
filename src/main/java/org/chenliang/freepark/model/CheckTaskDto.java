package org.chenliang.freepark.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CheckTaskDto {
  private Integer tenantId;
  private LocalDateTime startAt;
  private LocalDateTime lastCheckAt;
  private Integer checkCount;
  private Integer period;
}
