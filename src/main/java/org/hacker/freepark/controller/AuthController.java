package org.hacker.freepark.controller;

import org.hacker.freepark.model.CreateTenantRequest;
import org.hacker.freepark.model.LoginRequest;
import org.hacker.freepark.model.ResetPasswordRequest;
import org.hacker.freepark.model.TokenResponse;
import org.hacker.freepark.model.entity.Tenant;
import org.hacker.freepark.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
public class AuthController {
  @Autowired
  private AuthService authService;

  @PostMapping("/signup")
  public TokenResponse register(@Valid @RequestBody CreateTenantRequest request) {
    return authService.register(request);
  }


  @PostMapping("/login")
  public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @PutMapping("/password")
  public void resetPassword(@Valid @RequestBody ResetPasswordRequest request, @AuthenticationPrincipal Tenant tenant) {
    authService.resetPassword(request, tenant);
  }
}
