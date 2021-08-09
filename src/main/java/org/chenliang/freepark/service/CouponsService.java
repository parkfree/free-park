package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.exception.ProductNotFoundException;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.rtmap.ParkingCouponsResponse.Coupon;
import org.chenliang.freepark.model.rtmap.ProductsResponse;
import org.chenliang.freepark.model.rtmap.ProductsResponse.Product;
import org.chenliang.freepark.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
public class CouponsService {
  public static final String SEARCH_TITLE = "停车券";

  @Autowired
  private RtmapService rtmapService;

  @Autowired
  private MemberRepository memberRepository;

  public void buyParkingCoupons(Member member) {
    try {
      Product product = searchCurrentMonthParkingCouponProduct(member);
      rtmapService.buyParkingCoupons(member, product.getId(), product.getGetLimit());
    } finally {
      List<Coupon> coupons = getCurrentMonthParkingCoupons(member);
      member.setCoupons(coupons.size());
      memberRepository.save(member);
    }
  }

  public List<Coupon> updateAndGetCoupons(Member member) {
    List<Coupon> coupons = getCurrentMonthParkingCoupons(member);
    if (coupons.size() != member.getCoupons()) {
      member.setCoupons(coupons.size());
      memberRepository.save(member);
      log.info("Coupon count of member {} is not correct, update from {} to {}",
               member.getMobile(), member.getCoupons(), coupons.size());
    }
    return coupons;
  }

  public List<Coupon> getCurrentMonthParkingCoupons(Member member) {
    return rtmapService.getAccountCoupons(member).getData().getCouponList()
                       .stream()
                       .filter(coupon -> isCurrentMonthParkingCoupon(coupon.getMainInfo()))
                       .collect(Collectors.toList());
  }

  private Product searchCurrentMonthParkingCouponProduct(Member member) {
    ProductsResponse response = rtmapService.getProducts(member, 1);

    for (Product product : response.getData().getList()) {
      if (isCurrentMonthParkingCoupon(product.getMainInfo())) {
        return product;
      }
    }

    throw new ProductNotFoundException("Cannot find current month parking coupon product");
  }

  private boolean isCurrentMonthParkingCoupon(String productTitle) {
    String searchMonth = LocalDateTime.now().getMonthValue() + "月";
    return productTitle.contains(SEARCH_TITLE) && productTitle.contains(searchMonth);
  }
}
