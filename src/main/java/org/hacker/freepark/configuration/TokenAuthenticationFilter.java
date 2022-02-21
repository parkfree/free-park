package org.hacker.freepark.configuration;

import org.hacker.freepark.exception.TokenAuthenticationException;
import org.hacker.freepark.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

  @Autowired
  private AuthService authService;

  @Autowired
  TokenAuthenticationEntryPoint tokenAuthenticationEntryPoint;

  @Override
  protected void doFilterInternal(HttpServletRequest req,
                                  HttpServletResponse res,
                                  FilterChain chain) throws IOException, ServletException {
    String header = req.getHeader("Authorization");

    if (header == null || !header.startsWith(AuthService.TOKEN_PREFIX)) {
      chain.doFilter(req, res);
      return;
    }

    try {
      authService.authenticate(req).ifPresent(tenant -> {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(tenant.getRole()));
        Authentication authentication = new UsernamePasswordAuthenticationToken(tenant, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
      });
    } catch (TokenAuthenticationException e) {
      tokenAuthenticationEntryPoint.commence(req, res, e);
      return;
    }

    chain.doFilter(req, res);
  }
}
