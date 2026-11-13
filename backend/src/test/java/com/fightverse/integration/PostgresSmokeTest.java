package com.fightverse.integration;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * End-to-end smoke test against a real PostgreSQL instance.
 *
 * <p>This is the only test that exercises the parts a unit test cannot reach: Flyway migrations,
 * Hibernate's {@code ddl-auto: validate} agreement with the migrated schema, the real BCrypt hasher,
 * JWT encoding/decoding and the security filter chain.
 *
 * <p>{@code disabledWithoutDocker = true} makes the class skip rather than fail on machines without a
 * Docker daemon — that is the case in the CI sandbox this project currently has, so treat a skip as
 * "not yet verified against a database" rather than as a pass.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PostgresSmokeTest {

  /** Pinned so the migration and Hibernate validation run against a known server version. */
  private static final DockerImageName POSTGRES = DockerImageName.parse("postgres:17-alpine");

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES_CONTAINER = new PostgreSQLContainer(POSTGRES);

  private static final String REGISTER_BODY =
      """
      {"email":"Smoke.Test@Example.com","password":"correct-horse-battery","displayName":"  Smoke Test  "}
      """;

  @Autowired private MockMvc mockMvc;

  @Test
  void migrationsSeedTheReferenceCatalog() throws Exception {
    mockMvc
        .perform(get("/api/v1/disciplines").param("size", "100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(8))
        .andExpect(jsonPath("$.content[0].code").value("boxing"))
        .andExpect(
            jsonPath("$.content[*].code")
                .value(
                    containsInAnyOrder(
                        "boxing",
                        "karate",
                        "kickboxing",
                        "taekwondo",
                        "muay-thai",
                        "judo",
                        "brazilian-jiu-jitsu",
                        "wrestling")));
  }

  @Test
  void boxingTechniquesAreSeededAsPublished() throws Exception {
    mockMvc
        .perform(get("/api/v1/techniques").param("discipline", "boxing").param("size", "100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(
            jsonPath("$.content[*].code")
                .value(containsInAnyOrder("jab", "cross", "guard-recovery")));
  }

  @Test
  void seededTechniqueExposesItsEvaluationDefinition() throws Exception {
    String body =
        mockMvc
            .perform(get("/api/v1/techniques").param("discipline", "boxing"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = JsonPath.read(body, "$.content[0].id");

    mockMvc
        .perform(get("/api/v1/techniques/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PUBLISHED"))
        .andExpect(jsonPath("$.definitionSchemaVersion").value(1))
        .andExpect(jsonPath("$.movementDefinition").isNotEmpty())
        .andExpect(jsonPath("$.requiredLandmarks").isNotEmpty())
        .andExpect(jsonPath("$.evaluationCriteria").isNotEmpty())
        .andExpect(jsonPath("$.scoringRules.passingScore").value(70));
  }

  @Test
  void onlyBoxingHasContentSoFar() throws Exception {
    // Documents a known content gap rather than an aspiration: the other seven disciplines are empty.
    mockMvc
        .perform(get("/api/v1/techniques").param("discipline", "karate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void registrationIssuesAUsableAccessToken() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(REGISTER_BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.email").value("smoke.test@example.com"))
            .andExpect(jsonPath("$.user.displayName").value("Smoke Test"))
            .andExpect(jsonPath("$.user.status").value("ACTIVE"))
            .andExpect(jsonPath("$.user.roles[0]").value("STUDENT"))
            .andExpect(jsonPath("$.tokens.tokenType").value("Bearer"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String accessToken = JsonPath.read(body, "$.tokens.accessToken");

    mockMvc
        .perform(
            get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("smoke.test@example.com"))
        .andExpect(jsonPath("$.roles[0]").value("STUDENT"));
  }

  @Test
  void duplicateRegistrationIsAConflict() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER_BODY.replace("Smoke.Test", "Duplicate")))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER_BODY.replace("Smoke.Test", "Duplicate")))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/conflict")));
  }

  @Test
  void anonymousAccessToProtectedEndpointIsRejected() throws Exception {
    mockMvc
        .perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/unauthorized")));
  }
}
