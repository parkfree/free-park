package org.chenliang.freepark.scheduler;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.rtmap.ProductsResponse;
import org.chenliang.freepark.model.rtmap.ProductsResponse.Product;
import org.chenliang.freepark.repository.MemberRepository;
import org.chenliang.freepark.service.RtmapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GetCouponsTaskScheduler {

  @Value(value = "${coupon.name}")
  private String couponName;

  @Value(value = "${coupon.number}")
  private int couponNum;

  @Autowired
  private ThreadPoolTaskScheduler taskScheduler;

  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private RtmapService rtmapService;

  @Scheduled(cron = "${coupon.cron}")
  public void scheduleGetCouponsTask() {
    final List<Member> members = memberRepository.findAllCheckInAllowedMembers();
    Instant now = Instant.now();
    Random random = new Random();

    final int productId = getProductId(members.get(0));
    for (Member member : members) {
      int delaySeconds = random.nextInt(10);
      Instant startTime = now.plusSeconds(delaySeconds);
      log.info("Scheduled member {} to buy coupons at {}", member.getMobile(), startTime.toString());
      taskScheduler.schedule(() -> {
        try {
          rtmapService.buy(member, productId, couponNum);
          member.setCoupons(couponNum);
          memberRepository.save(member);
        } catch (Exception e) {
          log.info("Get coupons task for member {} failed with unexpected exception", member.getMobile(), e);
          member.setCoupons(0);
          memberRepository.save(member);
        }
      }, startTime);
    }
  }

  private int getProductId(Member member) {
    int startPage = 1;
    int productId = searchCouponId(member, startPage);
    if (productId == -1) {
      throw new RuntimeException("Cannot found free parking coupon product");
    }

    return productId;
  }

  private int searchCouponId(Member member, int page) {
    ProductsResponse response = rtmapService.getProducts(member, page);
    List<Product> products = response.getData().getList();
    for (Product product : products) {
      if (product.getMainInfo().contains(couponName)) {
        return product.getId();
      }
    }

    if ((page + 1) > response.getData().getPages()) {
      return -1;
    } else {
      return searchCouponId(member, page + 1);
    }
  }
}
