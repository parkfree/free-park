package org.hacker.freepark.controller.user;

import org.hacker.freepark.model.PaymentResponse;
import org.hacker.freepark.model.entity.Tenant;
import org.hacker.freepark.service.PaymentService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class PaymentController {
  @Autowired
  private PaymentService paymentService;

  @Autowired
  private ModelMapper modelMapper;

  @PostMapping("/payments")
  public PaymentResponse pay(@AuthenticationPrincipal Tenant tenant) {
    return modelMapper.map(paymentService.pay(tenant), PaymentResponse.class);
  }

  @GetMapping("/payments/today")
  public List<PaymentResponse> getPaymentsOfToday(@AuthenticationPrincipal Tenant tenant) {
    return paymentService.getTodayPayments(tenant)
                         .stream()
                         .map(payment -> modelMapper.map(payment, PaymentResponse.class))
                         .collect(Collectors.toList());
  }
}
