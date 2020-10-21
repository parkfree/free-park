package org.chenliang.freepark.controller.admin;

import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.PaymentDetailResponse;
import org.chenliang.freepark.model.PaymentResponse;
import org.chenliang.freepark.model.PaymentSearchQuery;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.repository.TenantRepository;
import org.chenliang.freepark.service.PaymentService;
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
    return paymentService.pay(tenant.getId());
  }

  @GetMapping("/tenants/{id}/payments/today")
  public List<PaymentResponse> getPaymentsOfToday(@PathVariable Integer id) {
    Tenant tenant = tenantRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    return paymentService.getTodayPayments(tenant);
  }
}
