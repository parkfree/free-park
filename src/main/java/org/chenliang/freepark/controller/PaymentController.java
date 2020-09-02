package org.chenliang.freepark.controller;

import org.chenliang.freepark.exception.PaymentErrorException;
import org.chenliang.freepark.model.PayStatus;
import org.chenliang.freepark.model.Tenant;
import org.chenliang.freepark.repository.TenantRepository;
import org.chenliang.freepark.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {
  @Autowired
  private PaymentService paymentService;

  @Autowired
  private TenantRepository tenantRepository;

  @PostMapping("/payment/tenant/{id}")
  public ResponseEntity<Void> pay(@PathVariable Integer id) {
    Tenant tenant = tenantRepository.getOne(id);
    PayStatus payStatus = paymentService.pay(tenant);
    if(payStatus == PayStatus.SUCCESS) {
      return ResponseEntity.ok().body(null);
    } else {
      throw new PaymentErrorException(payStatus);
    }
  }
}
