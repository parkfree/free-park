package org.hacker.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.hacker.freepark.exception.ProductNotFoundException;
import org.hacker.freepark.exception.ResourceNotFoundException;
import org.hacker.freepark.model.entity.Member;
import org.hacker.freepark.model.entity.Tenant;
import org.hacker.freepark.model.rtmap.ParkingCouponsResponse.Coupon;
import org.hacker.freepark.model.rtmap.ProductsResponse;
import org.hacker.freepark.model.rtmap.ProductsResponse.Product;
import org.hacker.freepark.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Log4j2
public class CouponsService {
  public static final String SEARCH_TITLE = "停车券";
  public static final Supplier<ResourceNotFoundException> MEMBER_NOT_FOUND = () -> new ResourceNotFoundException("Member not found");

  @Autowired
  private RtmapService rtmapService;

  @Autowired
  private MemberRepository memberRepository;

  public Member buyParkingCoupons(Tenant tenant, int memberId) {
    Member member = memberRepository.findFirstByIdAndTenantId(memberId, tenant.getId())
                                    .orElseThrow(MEMBER_NOT_FOUND);
    buyParkingCoupons(member);
    return memberRepository.findById(memberId)
                           .orElseThrow(MEMBER_NOT_FOUND);
  }

  public Member updateParkingCoupons(Tenant tenant, Integer memberId) {
    Member member = memberRepository.findFirstByIdAndTenantId(memberId, tenant.getId())
                                    .orElseThrow(MEMBER_NOT_FOUND);
    updateAndGetCoupons(member);
    return memberRepository.findById(memberId)
                           .orElseThrow(MEMBER_NOT_FOUND);
  }

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
      log.info("Coupon count of member {} is not correct, update from {} to {}",
               member.getMobile(), member.getCoupons(), coupons.size());
      member.setCoupons(coupons.size());
      memberRepository.save(member);
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
    for (int page = 1; ; page++) {
      ProductsResponse response = rtmapService.getProducts(member, page);

      for (Product product : response.getData().getList()) {
        if (isCurrentMonthParkingCoupon(product.getMainInfo())) {
          return product;
        }
      }
      if (response.getData().getPage() == response.getData().getPages()) {
        break;
      }
    }

    throw new ProductNotFoundException("Cannot find current month parking coupon product");
  }

  private boolean isCurrentMonthParkingCoupon(String productTitle) {
    String searchMonth = LocalDateTime.now().getMonthValue() + "月";
    return productTitle.contains(SEARCH_TITLE) && productTitle.contains(searchMonth);
  }
}
