package org.hacker.freepark.controller.admin;

import org.hacker.freepark.exception.ResourceNotFoundException;
import org.hacker.freepark.model.PaymentDetailResponse;
import org.hacker.freepark.model.PaymentResponse;
import org.hacker.freepark.model.PaymentSearchQuery;
import org.hacker.freepark.model.entity.Tenant;
import org.hacker.freepark.repository.TenantRepository;
import org.hacker.freepark.service.PaymentService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminPaymentController {
  @Autowired
  private PaymentService paymentService;

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private ModelMapper modelMapper;

  @GetMapping("/payments")
  public Page<PaymentDetailResponse> getPayments(Pageable pageable, PaymentSearchQuery searchQuery) {
    return paymentService.getPaymentsPage(pageable, searchQuery)
                         .map(payment -> modelMapper.map(payment, PaymentDetailResponse.class));
  }

  @PostMapping("/tenants/{id}/payments")
  public PaymentResponse pay(@PathVariable Integer id) {
    Tenant tenant = tenantRepository.findById(id)
                                    .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    return modelMapper.map(paymentService.pay(tenant), PaymentResponse.class);
  }

  @GetMapping("/tenants/{id}/payments/today")
  public List<PaymentResponse> getPaymentsOfToday(@PathVariable Integer id) {
    Tenant tenant = tenantRepository.findById(id)
                                    .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    return paymentService.getTodayPayments(tenant)
                         .stream()
                         .map(payment -> modelMapper.map(payment, PaymentResponse.class))
                         .collect(Collectors.toList());
  }
}
