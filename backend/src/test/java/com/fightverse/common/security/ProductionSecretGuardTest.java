package com.fightverse.common.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;

class ProductionSecretGuardTest {

  private final ProductionSecretGuard guard = new ProductionSecretGuard();

  @Test
  void rejectsTheDocumentedDevelopmentSecret() {
    ApplicationRunner runner =
        guard.rejectDevelopmentJwtSecret(
            properties("dev-only-secret-change-me-before-deploying-32"));

    assertThatThrownBy(() -> runner.run(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("FIGHTVERSE_JWT_SECRET");
  }

  @Test
  void rejectsAnySecretWithTheDevelopmentPrefix() {
    ApplicationRunner runner =
        guard.rejectDevelopmentJwtSecret(properties("dev-only-something-else-entirely-32"));

    assertThatThrownBy(() -> runner.run(null)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void allowsARealSecret() {
    ApplicationRunner runner =
        guard.rejectDevelopmentJwtSecret(properties("a-real-random-256-bit-secret-value"));

    assertThatCode(() -> runner.run(null)).doesNotThrowAnyException();
  }

  private static SecurityProperties properties(String jwtSecret) {
    return new SecurityProperties(
        "fightverse", jwtSecret, Duration.ofMinutes(15), Duration.ofDays(7));
  }
}
