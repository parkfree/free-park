package org.chenliang.freepark.service;

import java.time.Duration;
import java.util.Date;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

public class PayTrigger implements Trigger {

  private static final Duration PAY_PERIOD = Duration.ofMinutes(60);

  private static final int ONE_HOUR = 60 * 60 * 1000;

  private static final int MARKET_FREE_TIME = 20 * 60 * 1000;

  private static final int FORTY_MINUTES = 40 * 60 * 1000;

  private static final int PAY_TASK_DELAY = 5 * 1000;

  private final Integer parkTime;

  private int payTime = 0;

  public PayTrigger(Integer parkTime) {
    this.parkTime = parkTime;
  }

  @Override
  public Date nextExecutionTime(TriggerContext triggerContext) {
    if (payTime == 0) {
      return getFirstPayTime();
    } else if (payTime == 1) {
      return getSecondPayTime(triggerContext.lastScheduledExecutionTime());
    } else if (payTime == 2) {
      return getThirdPayTime(triggerContext.lastScheduledExecutionTime());
    } else {
      return new Date(triggerContext.lastScheduledExecutionTime().getTime() + ONE_HOUR);
    }
  }

  private Date getFirstPayTime() {
    Date now = new Date();
    payTime++;
    if (parkTime >= MARKET_FREE_TIME) {
      //大于20分钟，马上缴费
      return new Date(now.getTime() + PAY_TASK_DELAY);
    } else {
      //小于20分钟，等到20分种时缴费
      return new Date(now.getTime() + MARKET_FREE_TIME - parkTime + PAY_TASK_DELAY);
    }
  }

  private Date getSecondPayTime(Date lastScheduledExecutionTime) {
    payTime++;
    long time = lastScheduledExecutionTime.getTime();
    if (parkTime < MARKET_FREE_TIME) {
      //小于20分钟，在1个小时40分钟后缴费，因此第一次会在20分钟准时缴费
      time = time + ONE_HOUR + FORTY_MINUTES;
    } else if (parkTime < ONE_HOUR) {
      //小于1个小时大于20分钟，在2个小时的时候缴费
      time = time + ONE_HOUR * 2 - parkTime;
    } else {
      //大于1个小时，则在后一个小时执行
      int payPeriod = (int) PAY_PERIOD.toMinutes();
      long remindTime = payPeriod - parkTime % payPeriod;
      if (remindTime > MARKET_FREE_TIME) {
        time = time + remindTime;
      } else {
        //如果还在免费时间内，则还需要等20分钟到才能缴费
        time = time + MARKET_FREE_TIME;
      }
    }
    return new Date(time);
  }

  private Date getThirdPayTime(Date lastScheduledExecutionTime) {
    payTime++;
    int payPeriod = (int) PAY_PERIOD.toMinutes();
    long remindTime = payPeriod - parkTime % payPeriod;
    if (remindTime > MARKET_FREE_TIME) {
      return new Date(lastScheduledExecutionTime.getTime() + ONE_HOUR);
    } else {
      return new Date(lastScheduledExecutionTime.getTime() + ONE_HOUR - PAY_TASK_DELAY + remindTime);
    }
  }
}
