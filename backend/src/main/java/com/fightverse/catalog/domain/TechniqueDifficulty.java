package com.fightverse.catalog.domain;

import com.fightverse.common.error.InvalidRequestException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum TechniqueDifficulty {
  BEGINNER,
  INTERMEDIATE,
  ADVANCED,
  EXPERT;

  public static TechniqueDifficulty parse(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      String allowed = Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
      throw new InvalidRequestException(
          "Unknown difficulty '%s'. Allowed values: %s.".formatted(value, allowed));
    }
  }
}
