package com.fightverse.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class SecurityPropertiesTest {

  private static final String SECRET = "0123456789abcdef0123456789abcdef";

  @Test
  void defaultsIssuerWhenBlankOrNull() {
    assertThat(properties("  ", Duration.ofMinutes(15), Duration.ofDays(7)).issuer())
        .isEqualTo("fightverse");
    assertThat(properties(null, Duration.ofMinutes(15), Duration.ofDays(7)).issuer())
        .isEqualTo("fightverse");
  }

  @Test
  void keepsExplicitIssuer() {
    assertThat(properties("fightverse-test", Duration.ofMinutes(15), Duration.ofDays(7)).issuer())
        .isEqualTo("fightverse-test");
  }

  @Test
  void derivesHmacSha256KeyFromSecret() {
    SecretKey key =
        properties("fightverse", Duration.ofMinutes(15), Duration.ofDays(7)).jwtSecretKey();

    assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
    assertThat(key.getEncoded()).isEqualTo(SECRET.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void accessTokenTtlMustBeBetweenOneMinuteAndOneHour() {
    assertThat(isAccessTokenTtlSane(Duration.ofSeconds(59))).isFalse();
    assertThat(isAccessTokenTtlSane(Duration.ofSeconds(60))).isTrue();
    assertThat(isAccessTokenTtlSane(Duration.ofHours(1))).isTrue();
    assertThat(isAccessTokenTtlSane(Duration.ofSeconds(3601))).isFalse();
  }

  @Test
  void refreshTokenTtlMustBeBetweenOneHourAndOneYear() {
    assertThat(isRefreshTokenTtlSane(Duration.ofMinutes(59))).isFalse();
    assertThat(isRefreshTokenTtlSane(Duration.ofHours(1))).isTrue();
    assertThat(isRefreshTokenTtlSane(Duration.ofDays(365))).isTrue();
    assertThat(isRefreshTokenTtlSane(Duration.ofDays(366))).isFalse();
  }

  @Test
  void ttlChecksAreNullSafe() {
    SecurityProperties props = new SecurityProperties("fightverse", SECRET, null, null);

    assertThat(props.isAccessTokenTtlSane()).isFalse();
    assertThat(props.isRefreshTokenTtlSane()).isFalse();
  }

  private static boolean isAccessTokenTtlSane(Duration accessTokenTtl) {
    return properties("fightverse", accessTokenTtl, Duration.ofDays(7)).isAccessTokenTtlSane();
  }

  private static boolean isRefreshTokenTtlSane(Duration refreshTokenTtl) {
    return properties("fightverse", Duration.ofMinutes(15), refreshTokenTtl).isRefreshTokenTtlSane();
  }

  private static SecurityProperties properties(
      String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {
    return new SecurityProperties(issuer, SECRET, accessTokenTtl, refreshTokenTtl);
  }
}
