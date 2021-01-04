package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.rtmap.CouponsResponse;
import org.chenliang.freepark.model.rtmap.CouponsResponse.Coupon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class CouponsService {

  @Autowired
  private RtmapService rtmapService;

  public Coupon getOneCoupon(Member member) {
    CouponsResponse response = rtmapService.getCoupons(member);
    if (response.getData().getCouponList().isEmpty()) {
      return null;
    } else {
      return response.getData().getCouponList().get(0);
    }
  }
}
