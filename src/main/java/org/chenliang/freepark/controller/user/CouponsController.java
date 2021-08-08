package org.chenliang.freepark.controller.user;

import org.chenliang.freepark.model.entity.Tenant;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CouponsController {
  @PostMapping("/coupons")
  public ResponseEntity<Void> buyCoupons(@AuthenticationPrincipal Tenant tenant) {
    return ResponseEntity.ok().build();
  }
}
