package org.chenliang.freepark.configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class TaskSchedulerConfig {
  @Bean
  public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
    ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
    threadPoolTaskScheduler.setPoolSize(20);
    threadPoolTaskScheduler.setThreadNamePrefix("schedule-task");
    return threadPoolTaskScheduler;
  }

  @Bean
  public ScheduledExecutorService scheduledExecutorService() {
    ScheduledExecutorService service = Executors.newScheduledThreadPool(20);
    return service;
  }
}
