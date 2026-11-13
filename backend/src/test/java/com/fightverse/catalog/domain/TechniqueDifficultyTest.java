package com.fightverse.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fightverse.common.error.InvalidRequestException;
import org.junit.jupiter.api.Test;

class TechniqueDifficultyTest {

  @Test
  void returnsNullWhenAbsent() {
    assertThat(TechniqueDifficulty.parse(null)).isNull();
    assertThat(TechniqueDifficulty.parse("")).isNull();
    assertThat(TechniqueDifficulty.parse("   ")).isNull();
  }

  @Test
  void parsesCaseInsensitivelyAndTrims() {
    assertThat(TechniqueDifficulty.parse("beginner")).isEqualTo(TechniqueDifficulty.BEGINNER);
    assertThat(TechniqueDifficulty.parse("  INTERMEDIATE "))
        .isEqualTo(TechniqueDifficulty.INTERMEDIATE);
    assertThat(TechniqueDifficulty.parse("Expert")).isEqualTo(TechniqueDifficulty.EXPERT);
  }

  @Test
  void acceptsDashSeparatedSpelling() {
    assertThat(TechniqueDifficulty.parse("advanced")).isEqualTo(TechniqueDifficulty.ADVANCED);
  }

  @Test
  void rejectsUnknownValueAndListsAllowedValues() {
    assertThatThrownBy(() -> TechniqueDifficulty.parse("semi-pro"))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("semi-pro")
        .hasMessageContaining("BEGINNER, INTERMEDIATE, ADVANCED, EXPERT");
  }
}
