package org.hacker.freepark.controller.admin;

import org.hacker.freepark.exception.ResourceNotFoundException;
import org.hacker.freepark.model.CreateTenantRequest;
import org.hacker.freepark.model.TenantResponse;
import org.hacker.freepark.model.UpdateTenantRequest;
import org.hacker.freepark.model.entity.Tenant;
import org.hacker.freepark.repository.TenantRepository;
import org.hacker.freepark.service.TenantService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminTenantController {
  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private ModelMapper modelMapper;

  @GetMapping("/tenants/{id}")
  public TenantResponse getTenant(@PathVariable Integer id) {
    return tenantRepository.findById(id)
        .map(tenant -> modelMapper.map(tenant, TenantResponse.class))
        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
  }

  @GetMapping("/tenants")
  public List<TenantResponse> getTenants() {
    return tenantRepository.findAll().stream()
        .map(tenant -> modelMapper.map(tenant, TenantResponse.class))
        .collect(Collectors.toList());
  }

  @PostMapping("/tenants")
  public TenantResponse createTenant(@RequestBody @Validated CreateTenantRequest request) {
    Tenant tenant = tenantService.createTenant(request);
    return modelMapper.map(tenant, TenantResponse.class);
  }

  @PutMapping("/tenants/{id}")
  public TenantResponse updateTenant(@PathVariable Integer id, @RequestBody @Validated UpdateTenantRequest request) {
    Tenant tenant = tenantService.adminUpdateTenant(id, request);
    return modelMapper.map(tenant, TenantResponse.class);
  }
}
