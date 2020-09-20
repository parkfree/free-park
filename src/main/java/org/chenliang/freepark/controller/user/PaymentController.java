package org.chenliang.freepark.controller.user;

import org.chenliang.freepark.exception.PaymentErrorException;
import org.chenliang.freepark.model.PaymentStatus;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {
  @Autowired
  private PaymentService paymentService;

  @PostMapping("/payments")
  public ResponseEntity<Void> pay(@AuthenticationPrincipal Tenant tenant) {
    PaymentStatus paymentStatus = paymentService.pay(tenant);
    if (paymentStatus == PaymentStatus.SUCCESS) {
      return ResponseEntity.ok().body(null);
    } else {
      throw new PaymentErrorException(paymentStatus);
    }
  }

  @GetMapping("/payments/today")
  public ResponseEntity<Void> getPaymentsOfToday() {
    // implement detail
    return ResponseEntity.ok().build();
  }
}
