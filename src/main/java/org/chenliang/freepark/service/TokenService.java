package org.chenliang.freepark.service;

import org.chenliang.freepark.exception.InvalidRequestException;
import org.chenliang.freepark.exception.TokenAuthenticationException;
import org.chenliang.freepark.model.LoginRequest;
import org.chenliang.freepark.model.TokenResponse;
import org.chenliang.freepark.model.entity.AccessToken;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.repository.AccessTokenRepository;
import org.chenliang.freepark.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class TokenService {
  public static final String TOKEN_PREFIX = "Bearer ";
  public static final int TOKEN_EXPIRE_IN = 1296000; // 15 days

  private static final SecureRandom secureRandom = new SecureRandom();
  private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

  @Autowired
  private AccessTokenRepository accessTokenRepository;

  @Autowired
  private TenantRepository tenantRepository;

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

  public TokenResponse createToken(Tenant tenant) {
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
}
