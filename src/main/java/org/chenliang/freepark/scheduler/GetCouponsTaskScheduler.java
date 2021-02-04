package org.chenliang.freepark.scheduler;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.chenliang.freepark.exception.RtmapApiException;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.rtmap.ProductsResponse;
import org.chenliang.freepark.model.rtmap.ProductsResponse.Product;
import org.chenliang.freepark.repository.MemberRepository;
import org.chenliang.freepark.service.CouponsService;
import org.chenliang.freepark.service.RtmapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GetCouponsTaskScheduler {

  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private CouponsService couponsService;

  @Scheduled(cron = "${coupon.cron}")
  public void scheduleGetCouponsTask() {
    final List<Member> members = memberRepository.findAll();
    couponsService.buyCoupons(members);
  }

}
