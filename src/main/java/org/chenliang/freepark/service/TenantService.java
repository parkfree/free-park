package org.chenliang.freepark.service;

import org.apache.logging.log4j.util.Strings;
import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.CreateTenantRequest;
import org.chenliang.freepark.model.UpdateTenantRequest;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.repository.AccessTokenRepository;
import org.chenliang.freepark.repository.MemberRepository;
import org.chenliang.freepark.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {
  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private AccessTokenRepository accessTokenRepository;

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

  public Tenant adminUpdateTenant(Integer id, UpdateTenantRequest request) {
    Tenant tenant = tenantRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    tenant.setCarNumber(request.getCarNumber());
    tenant.setOwner(request.getOwner());
    tenant.setEmail(request.getEmail());
    if (Strings.isNotBlank(request.getPassword())) {
      tenant.setPassword(passwordEncoder.encode(request.getPassword()));
    }
    if (request.getRole() != null) {
      tenant.setRole(request.getRole());
    }
    return tenantRepository.save(tenant);
  }

  public Tenant updateCurrentLoginTenant(Tenant tenant, UpdateTenantRequest request) {
    tenant.setCarNumber(request.getCarNumber());
    tenant.setOwner(request.getOwner());
    tenant.setEmail(request.getEmail());
    return tenantRepository.save(tenant);
  }

  @Transactional
  public void deleteTenant(Integer tenantId) {
    if (!tenantRepository.existsById(tenantId)) {
      throw new ResourceNotFoundException("Tenant not found");
    }
    tenantRepository.deleteById(tenantId);
    memberRepository.deleteInBulkByTenantId(tenantId);
    accessTokenRepository.deleteInBulkByTenantId(tenantId);
  }
}
