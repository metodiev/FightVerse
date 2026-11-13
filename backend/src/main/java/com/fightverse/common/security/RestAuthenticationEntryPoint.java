package com.fightverse.common.security;

import com.fightverse.common.error.Problems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setHeader(
        HttpHeaders.WWW_AUTHENTICATE,
        authException instanceof InvalidBearerTokenException
            ? "Bearer error=\"invalid_token\""
            : "Bearer");

    ProblemDetail problem =
        Problems.of(
            HttpStatus.UNAUTHORIZED,
            "unauthorized",
            "Authentication required",
            "A valid access token is required to access this resource.",
            request.getRequestURI());
    response.getWriter().write(objectMapper.writeValueAsString(problem));
  }
}
