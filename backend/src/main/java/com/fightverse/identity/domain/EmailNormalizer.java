package com.fightverse.identity.domain;

import java.util.Locale;

public final class EmailNormalizer {

  private EmailNormalizer() {}

  /**
   * Emails are stored lowercase so lookups are case-insensitive without relying on a DB extension.
   */
  public static String normalize(String email) {
    return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
  }
}
