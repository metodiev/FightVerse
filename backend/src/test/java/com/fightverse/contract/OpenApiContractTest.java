package com.fightverse.contract;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fightverse.catalog.api.DisciplineController;
import com.fightverse.catalog.api.TechniqueController;
import com.fightverse.catalog.api.dto.CreateDisciplineRequest;
import com.fightverse.catalog.api.dto.DisciplineResponse;
import com.fightverse.catalog.api.dto.TechniqueResponse;
import com.fightverse.catalog.api.dto.TechniqueSummaryResponse;
import com.fightverse.catalog.application.DisciplineService;
import com.fightverse.catalog.application.TechniqueService;
import com.fightverse.catalog.domain.TechniqueDifficulty;
import com.fightverse.catalog.domain.TechniqueStatus;
import com.fightverse.common.api.PageResponse;
import com.fightverse.common.api.RequestIdFilter;
import com.fightverse.common.error.GlobalExceptionHandler;
import com.fightverse.common.error.ResourceNotFoundException;
import com.fightverse.identity.api.AuthController;
import com.fightverse.identity.api.UserController;
import com.fightverse.identity.api.dto.AuthSessionResponse;
import com.fightverse.identity.api.dto.TokenResponse;
import com.fightverse.identity.api.dto.UserResponse;
import com.fightverse.identity.application.AuthService;
import com.fightverse.identity.application.UserService;
import com.fightverse.identity.domain.Role;
import com.fightverse.identity.domain.User;
import com.fightverse.support.TestEntities;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.JsonNode;

/**
 * Pins the HTTP contract to {@code docs/api/openapi.yaml}, which the build copies onto the test
 * classpath. Every documented operation is exercised and both the request and the response are
 * validated against the specification, so an accidental field rename or status-code change fails
 * here rather than in a client.
 */
class OpenApiContractTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID DISCIPLINE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID TECHNIQUE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
  private static final String PASSWORD = "correct-horse-battery";
  private static final String REFRESH_TOKEN = "raw-refresh-token";

  private final AuthService authService = mock(AuthService.class);
  private final UserService userService = mock(UserService.class);
  private final DisciplineService disciplineService = mock(DisciplineService.class);
  private final TechniqueService techniqueService = mock(TechniqueService.class);

  private MockMvc mockMvc;
  private OpenApiInteractionValidator validator;

  @BeforeEach
  void setUp() {
    validator =
        OpenApiInteractionValidator.createForInlineApiSpecification(specification()).build();
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new AuthController(authService),
                new UserController(userService),
                new DisciplineController(disciplineService, techniqueService),
                new TechniqueController(techniqueService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new RequestIdFilter())
            .setMessageConverters(new JacksonJsonHttpMessageConverter(productionMapper()))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  /**
   * Mirrors the serialization settings from {@code application.yml} so the assertions describe what
   * production actually emits. Null fields are omitted (Jackson 3 already writes ISO-8601 instants)
   * and {@link ProblemDetail} is flattened the way Boot's auto-configuration does it, so custom
   * members such as {@code violations} and {@code requestId} sit at the top level instead of inside
   * a nested {@code properties} object.
   */
  private static JsonMapper productionMapper() {
    return JsonMapper.builder()
        .changeDefaultPropertyInclusion(
            inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
        .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
        .build();
  }

  private static RequestPostProcessor authenticated() {
    return request -> {
      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication());
      SecurityContextHolder.setContext(context);
      return request;
    };
  }

  @Test
  void listsDisciplines() throws Exception {
    when(disciplineService.list(0, 20))
        .thenReturn(new PageResponse<>(List.of(discipline()), 0, 20, 1, 1));

    mockMvc
        .perform(get("/api/v1/disciplines"))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid(validator));
  }

  @Test
  void getsDisciplineByCode() throws Exception {
    when(disciplineService.getByCode("boxing")).thenReturn(discipline());

    mockMvc
        .perform(get("/api/v1/disciplines/boxing"))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid(validator));
  }

  @Test
  void reportsMissingDisciplineAsProblemDetail() throws Exception {
    when(disciplineService.getByCode("unknown"))
        .thenThrow(new ResourceNotFoundException("Discipline 'unknown' was not found."));

    mockMvc
        .perform(get("/api/v1/disciplines/unknown"))
        .andExpect(status().isNotFound())
        .andExpect(openApi().isValid(validator));
  }

  @Test
  void listsTechniquesWithBothFilters() throws Exception {
    when(techniqueService.list("boxing", TechniqueDifficulty.BEGINNER, 0, 20))
        .thenReturn(new PageResponse<>(List.of(techniqueSummary()), 0, 20, 1, 1));

    mockMvc
        .perform(
            get("/api/v1/techniques")
                .param("discipline", "boxing")
                .param("difficulty", "beginner"))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid(validator));
  }

  @Test
  void getsTechniqueById() throws Exception {
    when(techniqueService.get(TECHNIQUE_ID)).thenReturn(technique());

    mockMvc
        .perform(get("/api/v1/techniques/{id}", TECHNIQUE_ID))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid(validator));
  }

  @Test
  void createsDiscipline() throws Exception {
    when(disciplineService.create(new CreateDisciplineRequest("muay-thai", "Muay Thai", null, 7)))
        .thenReturn(new DisciplineResponse(DISCIPLINE_ID, "muay-thai", "Muay Thai", null, 7));

    mockMvc
        .perform(
            post("/api/v1/disciplines")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"muay-thai","name":"Muay Thai","sortOrder":7}
                    """))
        .andExpect(status().isCreated())
        .andExpect(openApi().isValid(validator));
  }

  @Test
  void createsTechniqueInsideDiscipline() throws Exception {
    when(techniqueService.create(eq("boxing"), any())).thenReturn(technique());

    mockMvc
        .perform(
            post("/api/v1/disciplines/{code}/techniques", "boxing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(techniqueRequestJson()))
        .andExpect(status().isCreated())
        .andExpect(openApi().isValid(validator));
  }

  @Test
  void registersAccount() throws Exception {
    when(authService.register("fighter@example.com", PASSWORD, "Miro", "JUnit"))
        .thenReturn(session());

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .header("User-Agent", "JUnit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"fighter@example.com","password":"correct-horse-battery",
                     "displayName":"Miro"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(openApi().isValid(validator));
  }

  @Test
  void logsIn() throws Exception {
    when(authService.login("fighter@example.com", PASSWORD, null)).thenReturn(session());

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"fighter@example.com","password":"correct-horse-battery"}
                    """))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid(validator));
  }

  @Test
  void refreshesToken() throws Exception {
    when(authService.refresh(REFRESH_TOKEN, null)).thenReturn(tokens());

    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken":"raw-refresh-token"}
                    """))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid(validator));
  }

  @Test
  void logsOut() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .with(authenticated())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken":"raw-refresh-token"}
                    """))
        .andExpect(status().isNoContent())
        .andExpect(openApi().isValid(validator));
  }

  @Test
  void returnsCurrentUser() throws Exception {
    when(userService.getById(USER_ID)).thenReturn(user());

    mockMvc
        .perform(get("/api/v1/users/me").with(authenticated()))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid(validator));
  }

  @Test
  void reportsMalformedBodyAsMalformedRequest() throws Exception {
    // The validator cannot be used here: the request body is intentionally not valid JSON,
    // so request validation would fail before the response is ever inspected.
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not json"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.type").value(endsWith("/malformed-request")))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.instance").value("/api/v1/auth/login"));
  }

  @Test
  void reportsFieldValidationFailureAsValidationFailed() throws Exception {
    // Same reason as above: the request body deliberately violates the documented schema.
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"not-an-email","password":"short","displayName":""}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.type").value(endsWith("/validation-failed")))
        .andExpect(
            jsonPath("$.violations[*].field")
                .value(hasItems("email", "password", "displayName")));
  }

  private static String specification() {
    try (InputStream in = OpenApiContractTest.class.getResourceAsStream("/openapi.yaml")) {
      return new String(
          Objects.requireNonNull(in, "openapi.yaml must be on the test classpath").readAllBytes(),
          StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private static AbstractAuthenticationToken authentication() {
    Jwt jwt =
        Jwt.withTokenValue("access-token")
            .header("alg", "HS256")
            .subject(USER_ID.toString())
            .issuedAt(NOW)
            .expiresAt(NOW.plusSeconds(900))
            .claim("roles", List.of("ADMIN"))
            .build();
    return new JwtAuthenticationToken(
        jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")), "fighter@example.com");
  }

  private static User user() {
    return TestEntities.assignId(User.register("fighter@example.com", "hash", "Miro"), USER_ID);
  }

  private static TokenResponse tokens() {
    return new TokenResponse(
        "access-token",
        TokenResponse.BEARER,
        NOW.plusSeconds(900),
        REFRESH_TOKEN,
        NOW.plusSeconds(604_800));
  }

  private static AuthSessionResponse session() {
    return AuthSessionResponse.of(user(), tokens());
  }

  private static DisciplineResponse discipline() {
    return new DisciplineResponse(DISCIPLINE_ID, "boxing", "Boxing", "Sweet science", 10);
  }

  private static TechniqueSummaryResponse techniqueSummary() {
    return new TechniqueSummaryResponse(
        TECHNIQUE_ID,
        "boxing",
        "left-jab",
        "Left Jab",
        "Lead-hand straight punch",
        TechniqueDifficulty.BEGINNER);
  }

  private static TechniqueResponse technique() {
    JsonNode movement = JsonNodeFactory.instance.objectNode().put("schemaVersion", 1);
    JsonNode landmarks = JsonNodeFactory.instance.arrayNode().add("LEFT_SHOULDER");
    JsonNode criteria =
        JsonNodeFactory.instance
            .arrayNode()
            .add(
                JsonNodeFactory.instance
                    .objectNode()
                    .put("code", "ELBOW_EXTENSION")
                    .put("weight", 0.4));

    return new TechniqueResponse(
        TECHNIQUE_ID,
        "boxing",
        "left-jab",
        "Left Jab",
        "Lead-hand straight punch",
        TechniqueDifficulty.BEGINNER,
        TechniqueStatus.PUBLISHED,
        1,
        movement,
        landmarks,
        criteria,
        null,
        null,
        0L,
        NOW,
        NOW);
  }

  private static String techniqueRequestJson() {
    return """
        {
          "code": "left-jab",
          "name": "Left Jab",
          "description": "Lead-hand straight punch",
          "difficulty": "BEGINNER",
          "definitionSchemaVersion": 1,
          "movementDefinition": {"schemaVersion": 1, "primarySide": "LEFT"},
          "requiredLandmarks": ["LEFT_SHOULDER"],
          "evaluationCriteria": [{"code": "ELBOW_EXTENSION", "weight": 0.4}]
        }
        """;
  }
}
