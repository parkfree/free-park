package org.chenliang.freepark.controller;

import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.Tenant;
import org.chenliang.freepark.model.TenantDto;
import org.chenliang.freepark.repository.TenantRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class TenantController {
  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private ModelMapper modelMapper;

  @GetMapping("/tenants/{id}")
  public TenantDto getTenant(@PathVariable Integer id) {
    return tenantRepository.findById(id)
        .map(tenant -> modelMapper.map(tenant, TenantDto.class))
        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
  }

  @GetMapping("/tenants")
  public List<TenantDto> getTenants() {
    return tenantRepository.findAll().stream()
        .map(tenant -> modelMapper.map(tenant, TenantDto.class))
        .collect(Collectors.toList());
  }

  @PostMapping("/tenants")
  public TenantDto createTenant(@RequestBody TenantDto tenantDto) {
    Tenant tenant = modelMapper.map(tenantDto, Tenant.class);
    return modelMapper.map(tenantRepository.save(tenant), TenantDto.class);
  }

  @PutMapping("/tenants/{id}")
  public TenantDto createTenant(@PathVariable Integer id, @RequestBody TenantDto tenantDto) {
    Tenant tenant = tenantRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    tenant.setCarNumber(tenantDto.getCarNumber());
    tenant.setOwner(tenantDto.getOwner());
    tenant.setEmail(tenantDto.getEmail());
    return modelMapper.map(tenantRepository.save(tenant), TenantDto.class);
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
