package org.chenliang.freepark.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.repository.MemberRepository;
import org.chenliang.freepark.service.CouponsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class CouponTaskScheduler {

  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private ThreadPoolTaskScheduler taskScheduler;

  @Autowired
  private CouponsService couponsService;

  // start at 06:00 on day-of-month 1
  @Scheduled(cron = "0 0 6 1 * *}")
  public void scheduleGetCouponsTask() {
    List<Member> members = memberRepository.findByEnablePayIsTrue();
    Instant now = Instant.now();
    Random random = new Random();
    members.forEach(member -> {
      // Spreading the coupon tasks in next 2 hours
      int delaySeconds = random.nextInt(3600 * 2);
      Instant startTime = now.plusSeconds(delaySeconds);
      log.info("Scheduled member {} to buy coupons at {}", member.getMobile(), startTime.toString());
      taskScheduler.schedule(() -> {
        try {
          couponsService.buyParkingCoupons(member);
          log.info("Coupon task success for member {}", member.getMobile());
        } catch (Exception e) {
          log.info("Buy coupons task for member {} failed with unexpected exception", member.getMobile(), e);
        }
      }, startTime);
    });
  }

}
