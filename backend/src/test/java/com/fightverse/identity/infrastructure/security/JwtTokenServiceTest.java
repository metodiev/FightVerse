package com.fightverse.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fightverse.common.security.SecurityProperties;
import com.fightverse.identity.domain.Role;
import com.fightverse.identity.domain.User;
import com.fightverse.support.TestEntities;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

class JwtTokenServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final Duration ACCESS_TTL = Duration.ofMinutes(15);

  private final AtomicReference<JwtEncoderParameters> captured = new AtomicReference<>();
  private final JwtEncoder encoder =
      parameters -> {
        captured.set(parameters);
        return Jwt.withTokenValue("encoded-token").header("alg", "HS256").subject("subject").build();
      };
  private final SecurityProperties properties =
      new SecurityProperties(
          "fightverse", "0123456789abcdef0123456789abcdef", ACCESS_TTL, Duration.ofDays(7));
  private final JwtTokenService service =
      new JwtTokenService(encoder, properties, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void issuesTokenWithIdentityAndRoleClaims() {
    User user =
        TestEntities.assignId(
            User.register("fighter@example.com", "hash", "Miro"), USER_ID);
    user.grantRole(Role.ADMIN);

    JwtTokenService.IssuedAccessToken issued = service.issue(user);

    assertThat(issued.token()).isEqualTo("encoded-token");
    assertThat(issued.expiresAt()).isEqualTo(NOW.plus(ACCESS_TTL));
  }

  @Test
  void buildsExpectedClaimsAndHeader() {
    User user = TestEntities.assignId(User.register("fighter@example.com", "hash", "Miro"), USER_ID);
    user.grantRole(Role.ADMIN);

    service.issue(user);

    JwtEncoderParameters parameters = captured.get();
    JwtClaimsSet claims = parameters.getClaims();
    assertThat(claims.getClaimAsString("iss")).isEqualTo("fightverse");
    assertThat(claims.getSubject()).isEqualTo(USER_ID.toString());
    assertThat(claims.getIssuedAt()).isEqualTo(NOW);
    assertThat(claims.getExpiresAt()).isEqualTo(NOW.plus(ACCESS_TTL));
    assertThat(claims.getId()).isNotBlank();
    assertThat(claims.getClaimAsString(JwtTokenService.EMAIL_CLAIM))
        .isEqualTo("fighter@example.com");
    assertThat(claims.getClaimAsStringList(JwtTokenService.ROLES_CLAIM))
        .containsExactly("ADMIN", "STUDENT");

    JwsHeader header = parameters.getJwsHeader();
    assertThat(header.getAlgorithm()).isEqualTo(MacAlgorithm.HS256);
    assertThat(header.getType()).isEqualTo("JWT");
  }

  @Test
  void sortsRolesAlphabeticallyForStableTokens() {
    User user = TestEntities.assignId(User.register("fighter@example.com", "hash", "Miro"), USER_ID);
    user.grantRole(Role.INSTRUCTOR);
    user.grantRole(Role.ADMIN);

    service.issue(user);

    assertThat(captured.get().getClaims().getClaimAsStringList(JwtTokenService.ROLES_CLAIM))
        .containsExactly("ADMIN", "INSTRUCTOR", "STUDENT");
  }
}
