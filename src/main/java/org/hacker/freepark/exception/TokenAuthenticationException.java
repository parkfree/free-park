package org.hacker.freepark.exception;

import org.springframework.security.core.AuthenticationException;

public class TokenAuthenticationException extends AuthenticationException {
  public TokenAuthenticationException(String msg) {
    super(msg);
  }
}
