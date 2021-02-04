package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.exception.RtmapApiException;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.model.rtmap.CouponsResponse;
import org.chenliang.freepark.model.rtmap.CouponsResponse.Coupon;
import org.chenliang.freepark.model.rtmap.ProductsResponse;
import org.chenliang.freepark.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Random;

@Service
@Log4j2
public class CouponsService {

    @Value(value = "${coupon.name}")
    private String couponName;

    @Value(value = "${coupon.number}")
    private int couponNum;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private RtmapService rtmapService;

    @Autowired
    private MemberRepository memberRepository;

    public Coupon getOneCoupon(Member member) {
        CouponsResponse response = rtmapService.getCoupons(member);
        if (response.getData().getCouponList().isEmpty()) {
            return null;
        } else {
            return response.getData().getCouponList().get(0);
        }
    }

    public void buyCoupons(Tenant tenant) {
        List<Member> members = memberRepository.findByTenantId(tenant.getId());
        buyCoupons(members);
    }

    public void buyCoupons(List<Member> members) {
        Instant now = Instant.now();
        Random random = new Random();

        final int productId = getProductId(members.get(0));
        for (Member member : members) {
            int delaySeconds = random.nextInt(3600 * 2);
            Instant startTime = now.plusSeconds(delaySeconds);
            log.info("Scheduled member {} to buy coupons at {}", member.getMobile(), startTime.toString());
            taskScheduler.schedule(() -> {
                try {
                    rtmapService.buy(member, productId, couponNum);
                    member.setCoupons(couponNum);
                    memberRepository.save(member);
                } catch (RtmapApiException ignored) {
                } catch (Exception e) {
                    log.info("Get coupons task for member {} failed with unexpected exception", member.getMobile(), e);
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
        List<ProductsResponse.Product> products = response.getData().getList();
        for (ProductsResponse.Product product : products) {
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
