package com.fightverse.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class RestAccessDeniedHandlerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final RestAccessDeniedHandler handler = new RestAccessDeniedHandler(objectMapper);

  @Test
  void writesForbiddenProblemDetail() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/disciplines");
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.handle(request, response, new AccessDeniedException("missing ROLE_ADMIN"));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

    JsonNode body = objectMapper.readTree(response.getContentAsString());
    assertThat(body.get("status").asInt()).isEqualTo(403);
    assertThat(body.get("type").asText()).endsWith("/forbidden");
    assertThat(body.get("instance").asText()).isEqualTo("/api/v1/disciplines");
  }

  @Test
  void doesNotLeakTheAuthorizationFailureReason() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/disciplines");
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.handle(request, response, new AccessDeniedException("missing ROLE_ADMIN"));

    assertThat(response.getContentAsString()).doesNotContain("missing ROLE_ADMIN");
  }
}
