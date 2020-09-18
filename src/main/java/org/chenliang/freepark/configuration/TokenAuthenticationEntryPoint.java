package org.chenliang.freepark.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.exception.ErrorCodes;
import org.chenliang.freepark.model.ErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Log4j2
public class TokenAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
                       AuthenticationException exception) throws IOException, ServletException {
    log.warn("Unauthorized error: {}", exception.getMessage());
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    ErrorResponse error = ErrorResponse.builder()
        .code(ErrorCodes.UNAUTHORIZED)
        .message(exception.getMessage())
        .build();
    response.getOutputStream().println(objectMapper.writeValueAsString(error));
  }
}
