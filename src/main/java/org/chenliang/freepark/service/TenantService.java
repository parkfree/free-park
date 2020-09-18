package org.chenliang.freepark.service;

import org.apache.logging.log4j.util.Strings;
import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.CreateTenantRequest;
import org.chenliang.freepark.model.UpdateTenantRequest;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class TenantService {
  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private BCryptPasswordEncoder passwordEncoder;

  public Tenant createTenant(CreateTenantRequest request) {
    Tenant tenant = new Tenant();
    tenant.setRole(Tenant.DEFAULT_ROLE);
    tenant.setPassword(passwordEncoder.encode(request.getPassword()));
    tenant.setCarNumber(request.getCarNumber());
    tenant.setEmail(request.getEmail());
    tenant.setOwner(request.getOwner());

    return tenantRepository.save(tenant);
  }

  public Tenant updateTenant(Integer id, UpdateTenantRequest request) {
    Tenant tenant = tenantRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    tenant.setCarNumber(request.getCarNumber());
    tenant.setOwner(request.getOwner());
    tenant.setEmail(request.getEmail());
    if (request.getRole() != null) {
      tenant.setRole(request.getRole());
    }
    if (Strings.isNotBlank(request.getPassword())) {
      tenant.setPassword(passwordEncoder.encode(request.getPassword()));
    }
    return tenantRepository.save(tenant);
  }
}
