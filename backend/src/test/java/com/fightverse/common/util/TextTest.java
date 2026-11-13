package com.fightverse.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextTest {

  @Test
  void returnsNullForNull() {
    assertThat(Text.trimToNull(null)).isNull();
  }

  @Test
  void returnsNullForBlankValues() {
    assertThat(Text.trimToNull("")).isNull();
    assertThat(Text.trimToNull("   ")).isNull();
    assertThat(Text.trimToNull("\t\n ")).isNull();
  }

  @Test
  void trimsSurroundingWhitespace() {
    assertThat(Text.trimToNull("  left jab  ")).isEqualTo("left jab");
  }

  @Test
  void keepsAlreadyTrimmedValue() {
    assertThat(Text.trimToNull("left jab")).isEqualTo("left jab");
  }
}
