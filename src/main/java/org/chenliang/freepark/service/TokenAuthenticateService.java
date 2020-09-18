package org.chenliang.freepark.service;

import org.chenliang.freepark.exception.TokenAuthenticationException;
import org.chenliang.freepark.model.AccessToken;
import org.chenliang.freepark.model.Tenant;
import org.chenliang.freepark.repository.AccessTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TokenAuthenticateService {
  public static final String TOKEN_PREFIX = "Bearer ";

  @Autowired
  private AccessTokenRepository accessTokenRepository;

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
}
