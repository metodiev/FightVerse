package com.fightverse.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.fightverse.common.api.RequestIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class ProblemsTest {

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void buildsProblemDetailFromArguments() {
    ProblemDetail problem =
        Problems.of(HttpStatus.NOT_FOUND, "not-found", "Not found", "missing", "/api/v1/x");

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(problem.getTitle()).isEqualTo("Not found");
    assertThat(problem.getDetail()).isEqualTo("missing");
    assertThat(problem.getType()).hasToString("https://api.fightverse.app/problems/not-found");
    assertThat(problem.getInstance()).hasToString("/api/v1/x");
  }

  @Test
  void omitsInstanceWhenBlank() {
    ProblemDetail problem = Problems.of(HttpStatus.CONFLICT, "conflict", "Conflict", "detail", "  ");

    assertThat(problem.getInstance()).isNull();
  }

  @Test
  void addsRequestIdFromMdc() {
    MDC.put(RequestIdFilter.MDC_KEY, "abc-123");

    ProblemDetail problem = Problems.of(HttpStatus.BAD_REQUEST, "invalid", "Invalid", "detail", null);

    assertThat(problem.getProperties()).containsEntry("requestId", "abc-123");
  }

  @Test
  void omitsRequestIdWhenMdcIsEmpty() {
    ProblemDetail problem = Problems.of(HttpStatus.BAD_REQUEST, "invalid", "Invalid", "detail", null);

    assertThat(problem.getProperties()).isNullOrEmpty();
  }
}
