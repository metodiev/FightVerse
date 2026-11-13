package com.fightverse.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

class GlobalExceptionHandlerTest {

  private static final Validator VALIDATOR =
      Validation.buildDefaultValidatorFactory().getValidator();

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
  private final MockHttpServletRequest request =
      new MockHttpServletRequest("GET", "/api/v1/techniques/42");

  @Test
  void mapsNotFoundTo404() {
    ProblemDetail problem = handler.handleNotFound(new ResourceNotFoundException("gone"), request);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(problem.getDetail()).isEqualTo("gone");
    assertThat(problem.getInstance()).hasToString("/api/v1/techniques/42");
  }

  @Test
  void mapsConflictTo409() {
    ProblemDetail problem = handler.handleConflict(new ConflictException("duplicate"), request);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getDetail()).isEqualTo("duplicate");
  }

  @Test
  void mapsInvalidRequestTo400() {
    ProblemDetail problem =
        handler.handleInvalidRequest(new InvalidRequestException("bad field"), request);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getType()).hasToString("https://api.fightverse.app/problems/invalid-request");
  }

  @Test
  void mapsAuthenticationErrorsWithoutEchoingTheReason() {
    ProblemDetail problem =
        handler.handleAuthentication(
            new BadCredentialsException("user 'fighter@example.com' exists but password is wrong"),
            request);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(problem.getDetail()).isEqualTo("Invalid credentials.");
    assertThat(problem.getDetail()).doesNotContain("fighter@example.com");
  }

  @Test
  void mapsConstraintViolationsToList() {
    Set<ConstraintViolation<Sample>> violations = VALIDATOR.validate(new Sample(""));

    ProblemDetail problem =
        handler.handleConstraintViolation(new ConstraintViolationException(violations), request);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getType())
        .hasToString("https://api.fightverse.app/problems/validation-failed");
    assertThat(problem.getProperties()).containsKey("violations");

    @SuppressWarnings("unchecked")
    List<Map<String, String>> reported =
        (List<Map<String, String>>) problem.getProperties().get("violations");
    assertThat(reported)
        .singleElement()
        .satisfies(
            violation ->
                assertThat(violation)
                    .containsEntry("field", "name")
                    .containsEntry("message", "must not be blank"));
  }

  @Test
  void mapsDataIntegrityViolationsWithoutLeakingDatabaseDetails() {
    DataIntegrityViolationException failure =
        new DataIntegrityViolationException("duplicate key value violates unique constraint");

    ProblemDetail problem = handler.handleDataIntegrity(failure, request);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getDetail()).isEqualTo("The request conflicts with existing data.");
  }

  @Test
  void mapsUnexpectedErrorsTo500WithoutLeakingDetails() {
    ProblemDetail problem = handler.handleUnexpected(new IllegalStateException("secret"), request);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred. Please retry later.");
    assertThat(problem.getDetail()).doesNotContain("secret");
  }

  private static final class Sample {

    @NotBlank(message = "must not be blank")
    private final String name;

    private Sample(String name) {
      this.name = name;
    }
  }
}
