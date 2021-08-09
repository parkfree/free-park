package org.chenliang.freepark.controller.user;

import org.chenliang.freepark.model.MemberResponse;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.service.CouponsService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CouponsController {
  @Autowired
  private CouponsService couponsService;

  @Autowired
  private ModelMapper modelMapper;

  @PostMapping("/members/{id}/coupons")
  public MemberResponse buyCoupons(@PathVariable Integer id, @AuthenticationPrincipal Tenant tenant) {
    Member member = couponsService.buyParkingCoupons(tenant, id);
    return modelMapper.map(member, MemberResponse.class);
  }

  @PutMapping("/members/{id}/coupons")
  public MemberResponse updateCoupons(@PathVariable Integer id, @AuthenticationPrincipal Tenant tenant) {
    Member member = couponsService.updateParkingCoupons(tenant, id);
    return modelMapper.map(member, MemberResponse.class);
  }
}
