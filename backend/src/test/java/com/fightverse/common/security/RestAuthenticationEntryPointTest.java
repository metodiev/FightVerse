package com.fightverse.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class RestAuthenticationEntryPointTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final RestAuthenticationEntryPoint entryPoint =
      new RestAuthenticationEntryPoint(objectMapper);

  @Test
  void writesProblemDetailWithInvalidTokenChallenge() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(request, response, new InvalidBearerTokenException("expired"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE))
        .isEqualTo("Bearer error=\"invalid_token\"");
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

    JsonNode body = objectMapper.readTree(response.getContentAsString());
    assertThat(body.get("status").asInt()).isEqualTo(401);
    assertThat(body.get("type").asText()).endsWith("/unauthorized");
    assertThat(body.get("instance").asText()).isEqualTo("/api/v1/users/me");
  }

  @Test
  void usesPlainBearerChallengeForOtherAuthenticationFailures() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(request, response, new AuthenticationServiceException("backend down"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
  }

  @Test
  void doesNotLeakTheAuthenticationFailureReason() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(request, response, new InvalidBearerTokenException("jwt signature mismatch"));

    assertThat(response.getContentAsString()).doesNotContain("jwt signature mismatch");
  }
}
