package org.chenliang.freepark.controller.admin;

import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.CreateTenantRequest;
import org.chenliang.freepark.model.TenantResponse;
import org.chenliang.freepark.model.UpdateTenantRequest;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.repository.TenantRepository;
import org.chenliang.freepark.service.TenantService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class TenantAdminController {
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
  public TenantResponse createTenant(@RequestBody CreateTenantRequest request) {
    Tenant tenant = tenantService.createTenant(request);
    return modelMapper.map(tenant, TenantResponse.class);
  }

  @PutMapping("/tenants/{id}")
  public TenantResponse updateTenant(@PathVariable Integer id, @RequestBody UpdateTenantRequest request) {
    Tenant tenant = tenantService.adminUpdateTenant(id, request);
    return modelMapper.map(tenant, TenantResponse.class);
  }

  @DeleteMapping("/tenants/{id}")
  public ResponseEntity<Void> deleteTenant(@PathVariable Integer id) {
    if (!tenantRepository.existsById(id)) {
      throw new ResourceNotFoundException("Tenant not found");
    }
    tenantRepository.deleteById(id);
    return ResponseEntity.ok().build();
  }
}
