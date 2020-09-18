package org.chenliang.freepark.controller.user;

import org.chenliang.freepark.model.TenantResponse;
import org.chenliang.freepark.model.UpdateTenantRequest;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.service.TenantService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TenantController {
  @Autowired
  private TenantService tenantService;

  @Autowired
  private ModelMapper modelMapper;

  @GetMapping("/tenant")
  public TenantResponse getTenant(@AuthenticationPrincipal Tenant tenant) {
    return modelMapper.map(tenant, TenantResponse.class);
  }

  @PutMapping("/tenant")
  public TenantResponse updateTenant(@AuthenticationPrincipal Tenant tenant, @RequestBody UpdateTenantRequest request) {
    Tenant updatedTenant = tenantService.updateCurrentLoginTenant(tenant, request);
    return modelMapper.map(updatedTenant, TenantResponse.class);
  }
}
