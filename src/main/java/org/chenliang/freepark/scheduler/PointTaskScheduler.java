package org.chenliang.freepark.scheduler;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.exception.RtmapApiException;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.repository.MemberRepository;
import org.chenliang.freepark.service.PointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Random;

@Service
@Log4j2
public class PointTaskScheduler {
  @Autowired
  private PointService pointService;

  @Autowired
  private ThreadPoolTaskScheduler taskScheduler;

  @Autowired
  private MemberRepository memberRepository;

  @Scheduled(cron = "0 0 8 * * *")
  public void schedulePointsTask() {
    final List<Member> members = memberRepository.findAllCheckInAllowedMembers();
    Instant now = Instant.now();
    Random random = new Random();
    members.forEach(member -> {
      int delaySeconds = random.nextInt(3600 * 9);
      Instant startTime = now.plusSeconds(delaySeconds);
      log.info("Scheduled member {} to get checkin point at {}", member.getMobile(), startTime.toString());
      taskScheduler.schedule(() -> {
        try {
          pointService.checkInPoint(member.getId());
          log.info("Chekin point task success for member {}", member.getMobile());
        } catch (Exception e) {
          log.info("Point task for member {} failed with unexpected exception", member.getMobile(), e);
        }
      }, startTime);
    });
  }
}
