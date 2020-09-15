package org.chenliang.freepark.controller;

import org.chenliang.freepark.exception.InvalidRequestException;
import org.chenliang.freepark.model.*;
import org.chenliang.freepark.repository.AccessTokenRepository;
import org.chenliang.freepark.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@RestController
public class AuthController {
  private static final SecureRandom secureRandom = new SecureRandom();
  private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();
  public static final int TOKEN_EXPIRE_IN = 1296000; // 15 days

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private AccessTokenRepository accessTokenRepository;

  @Autowired
  private BCryptPasswordEncoder passwordEncoder;

  @PostMapping("/signup")
  public TokenResponse signUp(@RequestBody SignUpRequest request) {
    validateInviteCode(request.getInviteCode());
    Tenant tenant = createTenant(request);
    return createToken(tenant);
  }

  @PostMapping("/login")
  public TokenResponse login(@RequestBody LoginRequest request) {
    String email = request.getEmail();
    Tenant tenant = tenantRepository.findByEmail(email)
        .orElseThrow(() -> new InvalidRequestException("Invalid email or password"));
    if (passwordEncoder.matches(request.getPassword(), tenant.getPassword())) {
      return createToken(tenant);
    } else {
      throw new InvalidRequestException("Invalid email or password");
    }
  }

  private TokenResponse createToken(Tenant tenant) {
    String tokenString = randomToken();

    AccessToken accessToken = new AccessToken();
    accessToken.setToken(tokenString);
    accessToken.setExpireAt(LocalDateTime.now().plusSeconds(TOKEN_EXPIRE_IN));
    accessToken.setTenant(tenant);
    accessTokenRepository.save(accessToken);

    return TokenResponse.builder()
        .accessToken(tokenString)
        .expireIn(TOKEN_EXPIRE_IN)
        .build();
  }

  private String randomToken() {
    byte[] randomBytes = new byte[48];
    secureRandom.nextBytes(randomBytes);
    return base64Encoder.encodeToString(randomBytes);
  }

  private Tenant createTenant(@RequestBody SignUpRequest request) {
    Tenant tenant = new Tenant();
    tenant.setRole(Tenant.DEFAULT_ROLE);
    tenant.setPassword(passwordEncoder.encode(request.getPassword()));
    tenant.setCarNumber(request.getCarNumber());
    tenant.setEmail(request.getEmail());
    tenant.setOwner(request.getOwner());

    return tenantRepository.save(tenant);
  }

  private void validateInviteCode(String inviteCode) {
    if (!"FREEPARK20".equals(inviteCode)) {
      throw new InvalidRequestException("Invalid invite code");
    }
  }
}
