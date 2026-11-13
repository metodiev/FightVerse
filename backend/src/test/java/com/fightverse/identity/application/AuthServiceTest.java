package com.fightverse.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fightverse.common.error.ConflictException;
import com.fightverse.common.error.InvalidRequestException;
import com.fightverse.common.security.Secrets;
import com.fightverse.common.security.SecurityProperties;
import com.fightverse.identity.api.dto.AuthSessionResponse;
import com.fightverse.identity.api.dto.TokenResponse;
import com.fightverse.identity.domain.RefreshToken;
import com.fightverse.identity.domain.User;
import com.fightverse.identity.domain.UserStatus;
import com.fightverse.identity.infrastructure.persistence.RefreshTokenRepository;
import com.fightverse.identity.infrastructure.persistence.UserRepository;
import com.fightverse.identity.infrastructure.security.JwtTokenService;
import com.fightverse.support.TestEntities;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID OTHER_USER_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final String SECRET = "0123456789abcdef0123456789abcdef";
  private static final Duration ACCESS_TTL = Duration.ofMinutes(15);
  private static final Duration REFRESH_TTL = Duration.ofDays(7);

  private final UserRepository users = mock(UserRepository.class);
  private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
  private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
  private final PasswordEncoder passwordEncoder = spy(new PlainTextEncoder());
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

  private AuthService authService;

  @BeforeEach
  void setUp() {
    SecurityProperties properties =
        new SecurityProperties("fightverse", SECRET, ACCESS_TTL, REFRESH_TTL);
    authService =
        new AuthService(
            users, refreshTokens, passwordEncoder, jwtTokenService, properties, clock);

    when(jwtTokenService.issue(any(User.class)))
        .thenReturn(new JwtTokenService.IssuedAccessToken("access-token", NOW.plus(ACCESS_TTL)));
    when(refreshTokens.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void registerCreatesActiveUserAndIssuesTokens() {
    when(users.existsByEmail("fighter@example.com")).thenReturn(false);
    when(users.save(any(User.class)))
        .thenAnswer(invocation -> TestEntities.assignId(invocation.getArgument(0), USER_ID));

    AuthSessionResponse response =
        authService.register(" Fighter@Example.COM ", "correct-horse-battery", "  Miro  ", "agent");

    assertThat(response.user().id()).isEqualTo(USER_ID);
    assertThat(response.user().email()).isEqualTo("fighter@example.com");
    assertThat(response.user().displayName()).isEqualTo("Miro");
    assertThat(response.user().status()).isEqualTo(UserStatus.ACTIVE);
    assertThat(response.tokens().accessToken()).isEqualTo("access-token");
    assertThat(response.tokens().tokenType()).isEqualTo(TokenResponse.BEARER);
    assertThat(response.tokens().refreshToken()).isNotBlank();
    assertThat(response.tokens().refreshTokenExpiresAt()).isEqualTo(NOW.plus(REFRESH_TTL));

    ArgumentCaptor<RefreshToken> persisted = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokens).save(persisted.capture());
    assertThat(persisted.getValue().getTokenHash())
        .isEqualTo(Secrets.sha256Hex(response.tokens().refreshToken()));
  }

  @Test
  void registerRejectsDuplicateEmail() {
    when(users.existsByEmail("fighter@example.com")).thenReturn(true);

    assertThatThrownBy(
            () -> authService.register("fighter@example.com", "correct-horse-battery", "Miro", null))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already exists");

    verify(users, never()).save(any(User.class));
  }

  @Test
  void registerRejectsPasswordLongerThanBcryptLimitInBytes() {
    when(users.existsByEmail(anyString())).thenReturn(false);
    String tooLong = "a".repeat(73);

    assertThatThrownBy(
            () -> authService.register("fighter@example.com", tooLong, "Miro", null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("72 bytes");

    verify(users, never()).save(any(User.class));
  }

  @Test
  void registerCountsUtf8BytesWhenCheckingBcryptLimit() {
    when(users.existsByEmail(anyString())).thenReturn(false);
    // 40 * 2 bytes = 80 bytes, although only 40 characters.
    String twoByteCharacters = "\u00e9".repeat(40);

    assertThatThrownBy(
            () -> authService.register("fighter@example.com", twoByteCharacters, "Miro", null))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  void loginReturnsSessionForActiveUser() {
    User user = activeUser("secret");
    when(users.findByEmail("fighter@example.com")).thenReturn(Optional.of(user));

    AuthSessionResponse response = authService.login("Fighter@Example.com", "secret", "agent");

    assertThat(response.user().id()).isEqualTo(USER_ID);
    assertThat(response.tokens().accessToken()).isEqualTo("access-token");
    assertThat(response.tokens().refreshToken()).isNotBlank();
  }

  @Test
  void loginHashesEvenWhenAccountIsMissing() {
    when(users.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login("ghost@example.com", "secret", null))
        .isInstanceOf(BadCredentialsException.class);

    verify(passwordEncoder).matches(eq("secret"), anyString());
  }

  @Test
  void loginRejectsWrongPassword() {
    User user = activeUser("secret");
    when(users.findByEmail("fighter@example.com")).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> authService.login("fighter@example.com", "wrong", null))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void loginRejectsInactiveAccountAfterPasswordCheck() {
    User disabled = mock(User.class);
    when(disabled.getPasswordHash()).thenReturn("enc:secret");
    when(disabled.getStatus()).thenReturn(UserStatus.DISABLED);
    when(users.findByEmail("fighter@example.com")).thenReturn(Optional.of(disabled));

    assertThatThrownBy(() -> authService.login("fighter@example.com", "secret", null))
        .isInstanceOf(DisabledException.class);
  }

  @Test
  void refreshRevokesOldTokenAndIssuesNewPair() {
    String rawToken = "raw-refresh-token";
    RefreshToken stored =
        RefreshToken.issue(
            activeUser("secret"),
            Secrets.sha256Hex(rawToken),
            NOW.plus(REFRESH_TTL),
            "agent");
    when(refreshTokens.findByTokenHash(Secrets.sha256Hex(rawToken)))
        .thenReturn(Optional.of(stored));

    TokenResponse response = authService.refresh(rawToken, "agent");

    assertThat(stored.getRevokedAt()).isEqualTo(NOW);
    assertThat(response.accessToken()).isEqualTo("access-token");
    assertThat(response.refreshToken()).isNotEqualTo(rawToken);
    assertThat(response.refreshTokenExpiresAt()).isEqualTo(NOW.plus(REFRESH_TTL));
  }

  @Test
  void refreshRejectsUnknownToken() {
    when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.refresh("unknown", null))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void refreshDropsEverySessionWhenRevokedTokenIsReused() {
    String rawToken = "replayed-token";
    RefreshToken stored =
        RefreshToken.issue(
            activeUser("secret"), Secrets.sha256Hex(rawToken), NOW.plus(REFRESH_TTL), "agent");
    stored.revoke(NOW.minusSeconds(60));
    when(refreshTokens.findByTokenHash(Secrets.sha256Hex(rawToken)))
        .thenReturn(Optional.of(stored));
    when(refreshTokens.revokeAllActiveForUser(USER_ID, NOW)).thenReturn(2);

    assertThatThrownBy(() -> authService.refresh(rawToken, null))
        .isInstanceOf(BadCredentialsException.class);

    verify(refreshTokens).revokeAllActiveForUser(USER_ID, NOW);
  }

  @Test
  void refreshRejectsExpiredToken() {
    String rawToken = "expired-token";
    RefreshToken stored =
        RefreshToken.issue(activeUser("secret"), Secrets.sha256Hex(rawToken), NOW, "agent");
    when(refreshTokens.findByTokenHash(Secrets.sha256Hex(rawToken)))
        .thenReturn(Optional.of(stored));

    assertThatThrownBy(() -> authService.refresh(rawToken, null))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void logoutRevokesTokenOwnedByCaller() {
    String rawToken = "logout-token";
    RefreshToken stored =
        RefreshToken.issue(
            activeUser("secret"), Secrets.sha256Hex(rawToken), NOW.plus(REFRESH_TTL), "agent");
    when(refreshTokens.findByTokenHash(Secrets.sha256Hex(rawToken)))
        .thenReturn(Optional.of(stored));

    authService.logout(USER_ID, rawToken);

    assertThat(stored.getRevokedAt()).isEqualTo(NOW);
  }

  @Test
  void logoutIgnoresTokenOwnedByAnotherUser() {
    String rawToken = "foreign-token";
    User other =
        TestEntities.assignId(
            User.register("other@example.com", "enc:secret", "Other"), OTHER_USER_ID);
    RefreshToken stored =
        RefreshToken.issue(other, Secrets.sha256Hex(rawToken), NOW.plus(REFRESH_TTL), "agent");
    when(refreshTokens.findByTokenHash(Secrets.sha256Hex(rawToken)))
        .thenReturn(Optional.of(stored));

    authService.logout(USER_ID, rawToken);

    assertThat(stored.getRevokedAt()).isNull();
  }

  @Test
  void logoutIgnoresUnknownToken() {
    when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.empty());

    authService.logout(USER_ID, "unknown");

    verify(refreshTokens, never()).revokeAllActiveForUser(any(), any());
  }

  private User activeUser(String rawPassword) {
    return TestEntities.assignId(
        User.register("fighter@example.com", passwordEncoder.encode(rawPassword), "Miro"), USER_ID);
  }

  private static final class PlainTextEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
      return "enc:" + rawPassword;
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
      return encodedPassword != null && encodedPassword.equals(encode(rawPassword));
    }
  }
}
