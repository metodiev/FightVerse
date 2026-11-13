package com.fightverse.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fightverse.support.TestEntities;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant LATER = NOW.plusSeconds(3600);

  private final User user =
      TestEntities.assignId(
          User.register("fighter@example.com", "hash", "Miro"),
          UUID.fromString("11111111-1111-1111-1111-111111111111"));

  @Test
  void issueStoresHashExpiryAndUserAgent() {
    RefreshToken token = RefreshToken.issue(user, "hash", LATER, "Mozilla/5.0");

    assertThat(token.getUser()).isSameAs(user);
    assertThat(token.getTokenHash()).isEqualTo("hash");
    assertThat(token.getExpiresAt()).isEqualTo(LATER);
    assertThat(token.getUserAgent()).isEqualTo("Mozilla/5.0");
    assertThat(token.getRevokedAt()).isNull();
  }

  @Test
  void issueAcceptsNullUserAgent() {
    assertThat(RefreshToken.issue(user, "hash", LATER, null).getUserAgent()).isNull();
  }

  @Test
  void issueTruncatesUserAgentToColumnLength() {
    String tooLong = "x".repeat(600);

    RefreshToken token = RefreshToken.issue(user, "hash", LATER, tooLong);

    assertThat(token.getUserAgent()).hasSize(512);
  }

  @Test
  void revokeRecordsFirstRevocationOnly() {
    RefreshToken token = RefreshToken.issue(user, "hash", LATER, "agent");

    token.revoke(NOW);
    token.revoke(LATER);

    assertThat(token.getRevokedAt()).isEqualTo(NOW);
  }

  @Test
  void isActiveIsTrueBeforeExpiry() {
    RefreshToken token = RefreshToken.issue(user, "hash", LATER, "agent");

    assertThat(token.isActive(NOW.plusSeconds(1))).isTrue();
  }

  @Test
  void isActiveIsFalseAtOrAfterExpiry() {
    RefreshToken token = RefreshToken.issue(user, "hash", LATER, "agent");

    assertThat(token.isActive(LATER)).isFalse();
    assertThat(token.isActive(LATER.plusSeconds(1))).isFalse();
  }

  @Test
  void isActiveIsFalseWhenRevoked() {
    RefreshToken token = RefreshToken.issue(user, "hash", LATER, "agent");

    token.revoke(NOW);

    assertThat(token.isActive(NOW)).isFalse();
  }

  @Test
  void belongsToComparesOwnerId() {
    RefreshToken token = RefreshToken.issue(user, "hash", LATER, "agent");

    assertThat(token.belongsTo(user.getId())).isTrue();
    assertThat(token.belongsTo(UUID.fromString("22222222-2222-2222-2222-222222222222"))).isFalse();
  }
}
