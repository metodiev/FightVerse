package com.fightverse.identity.api.dto;

import java.time.Instant;

public record TokenResponse(
    String accessToken,
    String tokenType,
    Instant accessTokenExpiresAt,
    String refreshToken,
    Instant refreshTokenExpiresAt) {

  public static final String BEARER = "Bearer";
}
