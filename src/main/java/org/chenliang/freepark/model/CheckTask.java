package org.chenliang.freepark.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;

@Data
@Builder
public class CheckTask {
    private Tenant tenant;
    private LocalDateTime startAt;
    private LocalDateTime lastCheckAt;
    private Integer checkCount;
    private Integer period;
    @JsonIgnore
    private ScheduledFuture<?> future;
}
