package org.chenliang.freepark.controller.user;

import org.chenliang.freepark.model.MemberResponse;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.service.PointService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PointController {
  @Autowired
  private PointService pointService;

  @Autowired
  private ModelMapper modelMapper;

  @PostMapping("/members/{id}/points")
  public MemberResponse buyCoupons(@PathVariable Integer id, @AuthenticationPrincipal Tenant tenant) {
    Member member = pointService.checkInPoint(tenant, id);
    return modelMapper.map(member, MemberResponse.class);
  }

  @PutMapping("/members/{id}/points")
  public MemberResponse updateCoupons(@PathVariable Integer id, @AuthenticationPrincipal Tenant tenant) {
    Member member = pointService.updatePoint(tenant, id);
    return modelMapper.map(member, MemberResponse.class);
  }
}
