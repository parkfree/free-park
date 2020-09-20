package org.chenliang.freepark.controller;

import org.chenliang.freepark.model.CreateTenantRequest;
import org.chenliang.freepark.model.LoginRequest;
import org.chenliang.freepark.model.TokenResponse;
import org.chenliang.freepark.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
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

}
