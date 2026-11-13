package com.fightverse.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class WebPropertiesTest {

  @Test
  void defaultsToEmptyListWhenMissing() {
    assertThat(new WebProperties(null).corsAllowedOrigins()).isEmpty();
  }

  @Test
  void keepsConfiguredOrigins() {
    WebProperties properties = new WebProperties(List.of("https://fightverse.app"));

    assertThat(properties.corsAllowedOrigins()).containsExactly("https://fightverse.app");
  }

  @Test
  void copiesOriginsIntoImmutableList() {
    List<String> mutable = new java.util.ArrayList<>(List.of("https://fightverse.app"));

    WebProperties properties = new WebProperties(mutable);
    mutable.add("https://evil.example");

    assertThat(properties.corsAllowedOrigins()).containsExactly("https://fightverse.app");
    assertThatThrownBy(() -> properties.corsAllowedOrigins().add("https://evil.example"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
