package org.chenliang.freepark.controller;

import org.chenliang.freepark.exception.InvalidRequestException;
import org.chenliang.freepark.model.LoginRequest;
import org.chenliang.freepark.model.CreateTenantRequest;
import org.chenliang.freepark.model.TokenResponse;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.service.TenantService;
import org.chenliang.freepark.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
public class AuthController {
  @Autowired
  private TenantService tenantService;

  @Autowired
  private TokenService tokenService;

  @PostMapping("/signup")
  public TokenResponse signUp(@Valid @RequestBody CreateTenantRequest request) {
    validateInviteCode(request.getInviteCode());
    Tenant tenant = tenantService.createTenant(request);
    return tokenService.createToken(tenant);
  }

  @PostMapping("/login")
  public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return tokenService.login(request);
  }

  private void validateInviteCode(String inviteCode) {
    if (!"FREEPARK20".equals(inviteCode)) {
      throw new InvalidRequestException("Invalid invite code");
    }
  }
}
