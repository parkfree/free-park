package org.chenliang.freepark.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.chenliang.freepark.model.PayTask;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

@Slf4j
public class PayTrigger implements Trigger {

  private static final int ONE_HOUR = 60 * 60 * 1000;

  private static final int MARKET_FREE_TIME = 20 * 60 * 1000;

  private static final int FORTY_MINUTES = 40 * 60 * 1000;

  private int payCount = 0;

  private String carNumber;

  private PayTask payTask;

  private int payTimeDelay;

  private long parkAtTimestamp;

  public PayTrigger(long parkAtTimestamp, String carNumber, PayTask payTask) {
    this.parkAtTimestamp = parkAtTimestamp;
    this.carNumber = carNumber;
    this.payTask = payTask;
    this.payTimeDelay = getDelaySeconds(parkAtTimestamp);
  }

  @Override
  public Date nextExecutionTime(TriggerContext triggerContext) {
    Date lastScheduledDate = triggerContext.lastScheduledExecutionTime();
    long nextPayTimestamp;
    if (payCount == 0) {
      nextPayTimestamp = getFirstPayTimestamp();
    } else if (payCount == 1) {
      nextPayTimestamp = getSecondPayTimestamp(lastScheduledDate);
    } else if (payCount == 2) {
      nextPayTimestamp = getThirdPayTimestamp(lastScheduledDate);
    } else {
      nextPayTimestamp = lastScheduledDate.getTime() + ONE_HOUR;
    }
    LocalDateTime nextPayDateTime = Instant.ofEpochMilli(nextPayTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
    payTask.setNextScheduledAt(nextPayDateTime);
    Date nextPayDate = new Date();
    nextPayDate.setTime(nextPayTimestamp);
    log.info("{} next pay will be at date: {}", carNumber, nextPayDate);
    payCount++;
    return nextPayDate;
  }

  private long getFirstPayTimestamp() {
    Date now = new Date();
    if ((now.getTime() - parkAtTimestamp) <= (MARKET_FREE_TIME + payTimeDelay)) {
      return parkAtTimestamp + MARKET_FREE_TIME + payTimeDelay;
    } else {
      return now.getTime();
    }
  }

  private long getSecondPayTimestamp(Date lastScheduledDate) {
    Long parkedTime = lastScheduledDate.getTime() - parkAtTimestamp;
    if (parkedTime <= (ONE_HOUR + FORTY_MINUTES)) {
      return parkAtTimestamp + ONE_HOUR * 2 + payTimeDelay;
    } else {
      long durationTime = parkedTime % ONE_HOUR;
      if (durationTime >= FORTY_MINUTES) {
        return lastScheduledDate.getTime() + MARKET_FREE_TIME;
      } else {
        return lastScheduledDate.getTime() + ONE_HOUR - durationTime + payTimeDelay;
      }
    }
  }

  private long getThirdPayTimestamp(Date lastScheduledDate) {
    Long remindTime = lastScheduledDate.getTime() - parkAtTimestamp;
    long durationTime = remindTime % ONE_HOUR;
    if (durationTime <= payTimeDelay) {
      return lastScheduledDate.getTime() + ONE_HOUR;
    } else {
      return lastScheduledDate.getTime() + ONE_HOUR - durationTime + payTimeDelay;
    }
  }

  private int getDelaySeconds(long parkAtTimestamp) {
    Date date = new Date();
    date.setTime(parkAtTimestamp);
    return (60 - date.getSeconds()) * 1000;
  }
}
