package org.chenliang.freepark.controller.user;

import org.chenliang.freepark.model.TenantResponse;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.service.CouponsService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class CouponsController {
  @Autowired
  private CouponsService couponsService;

  @Autowired
  private ModelMapper modelMapper;

  @PostMapping("/coupons")
  public ResponseEntity<Void> buyCoupons(@AuthenticationPrincipal Tenant tenant) {
    couponsService.buyCoupons(tenant);
    return ResponseEntity.ok().build();
  }
}
