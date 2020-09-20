package org.chenliang.freepark.service;

import org.chenliang.freepark.exception.InvalidRequestException;
import org.chenliang.freepark.exception.TokenAuthenticationException;
import org.chenliang.freepark.model.CreateTenantRequest;
import org.chenliang.freepark.model.LoginRequest;
import org.chenliang.freepark.model.ResetPasswordRequest;
import org.chenliang.freepark.model.TokenResponse;
import org.chenliang.freepark.model.entity.AccessToken;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.repository.AccessTokenRepository;
import org.chenliang.freepark.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthService {
  public static final String TOKEN_PREFIX = "Bearer ";
  public static final Duration TOKEN_EXPIRE_IN = Duration.ofDays(15); // 15 days

  private static final SecureRandom secureRandom = new SecureRandom();
  private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

  @Value("${invite-code}")
  private String defaultInviteCode;

  @Autowired
  private AccessTokenRepository accessTokenRepository;

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private BCryptPasswordEncoder passwordEncoder;

  @Transactional
  public Optional<Tenant> authenticate(HttpServletRequest request) {
    return Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
        .map(token -> token.replaceFirst(TOKEN_PREFIX, ""))
        .map(token -> {
          AccessToken accessToken = accessTokenRepository.findByToken(token)
              .orElseThrow(() -> new TokenAuthenticationException("Token not found"));

          if (accessToken.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new TokenAuthenticationException("Expired token");
          }

          return accessToken;
        })
        .map(AccessToken::getTenant);
  }

  @Transactional
  public TokenResponse register(@RequestBody @Valid CreateTenantRequest request) {
    validateInviteCode(request.getInviteCode());
    Tenant tenant = tenantService.createTenant(request);
    return createToken(tenant);
  }

  public TokenResponse login(LoginRequest request) {
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
    accessToken.setExpireAt(LocalDateTime.now().plus(TOKEN_EXPIRE_IN));
    accessToken.setTenant(tenant);
    accessTokenRepository.save(accessToken);

    return TokenResponse.builder()
        .accessToken(tokenString)
        .expireIn((int) TOKEN_EXPIRE_IN.toSeconds())
        .build();
  }

  private String randomToken() {
    byte[] randomBytes = new byte[48];
    secureRandom.nextBytes(randomBytes);
    return base64Encoder.encodeToString(randomBytes);
  }

  private void validateInviteCode(String inviteCode) {
    if (!defaultInviteCode.equals(inviteCode)) {
      throw new InvalidRequestException("Invalid invite code");
    }
  }

  public void resetPassword(ResetPasswordRequest request, Tenant tenant) {
    if (!passwordEncoder.matches(request.getOldPassword(), tenant.getPassword())) {
      throw new InvalidRequestException("Invalid old password");
    }
    tenant.setPassword(passwordEncoder.encode(request.getNewPassword()));
    tenantRepository.save(tenant);
  }
}
