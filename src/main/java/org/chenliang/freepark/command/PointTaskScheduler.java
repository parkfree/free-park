package org.chenliang.freepark.command;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.repository.MemberRepository;
import org.chenliang.freepark.service.RtmapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;
import java.util.Random;

@Service
@Log4j2
public class PointTaskScheduler {
  @Autowired
  private RtmapService rtmapService;

  @Autowired
  private ThreadPoolTaskScheduler taskScheduler;

  @Autowired
  private MemberRepository memberRepository;

  @Scheduled(cron = "0 0 8 * * *")
  public void schedulePointTask() {
    final List<Member> members = memberRepository.findByEnablePointIsTrue();
    Instant now = Instant.now();
    Random random = new Random();
    members.forEach(member -> {
      int delaySeconds = random.nextInt(3600 * 12);
      taskScheduler.schedule(() -> rtmapService.getPoint(member), now.plusSeconds(delaySeconds));
    });
  }
}
