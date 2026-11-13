package com.fightverse.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailNormalizerTest {

  @Test
  void returnsNullForNull() {
    assertThat(EmailNormalizer.normalize(null)).isNull();
  }

  @Test
  void trimsAndLowercases() {
    assertThat(EmailNormalizer.normalize("  Fighter@Example.COM  "))
        .isEqualTo("fighter@example.com");
  }

  @Test
  void keepsAlreadyNormalizedValue() {
    assertThat(EmailNormalizer.normalize("fighter@example.com")).isEqualTo("fighter@example.com");
  }

  @Test
  void lowercasesEveryCharacterRegardlessOfCase() {
    assertThat(EmailNormalizer.normalize("ADMIN@FIGHTVERSE.APP"))
        .isEqualTo("admin@fightverse.app");
  }
}
