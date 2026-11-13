package com.fightverse.common.security;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fightverse.security")
public record SecurityProperties(
    @NotBlank String issuer,
    @NotBlank
        @Size(min = 32, message = "must be at least 32 characters so HMAC-SHA256 has a 256-bit key")
        String jwtSecret,
    @NotNull Duration accessTokenTtl,
    @NotNull Duration refreshTokenTtl) {

  public static final String DEVELOPMENT_JWT_SECRET = "dev-only-secret-change-me-before-deploying-32";

  public SecurityProperties {
    if (issuer == null || issuer.isBlank()) {
      issuer = "fightverse";
    }
  }

  public SecretKey jwtSecretKey() {
    return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  @AssertTrue(message = "access token TTL must be between 60 seconds and 1 hour")
  public boolean isAccessTokenTtlSane() {
    return accessTokenTtl != null
        && accessTokenTtl.toSeconds() >= 60
        && accessTokenTtl.toSeconds() <= 3600;
  }

  @AssertTrue(message = "refresh token TTL must be between 1 hour and 365 days")
  public boolean isRefreshTokenTtlSane() {
    return refreshTokenTtl != null
        && refreshTokenTtl.toHours() >= 1
        && refreshTokenTtl.toDays() <= 365;
  }
}
