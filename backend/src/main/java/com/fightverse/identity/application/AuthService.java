package com.fightverse.identity.application;

import com.fightverse.common.error.ConflictException;
import com.fightverse.common.error.InvalidRequestException;
import com.fightverse.common.security.Secrets;
import com.fightverse.common.security.SecurityProperties;
import com.fightverse.identity.api.dto.AuthSessionResponse;
import com.fightverse.identity.api.dto.TokenResponse;
import com.fightverse.identity.domain.EmailNormalizer;
import com.fightverse.identity.domain.RefreshToken;
import com.fightverse.identity.domain.User;
import com.fightverse.identity.domain.UserStatus;
import com.fightverse.identity.infrastructure.persistence.RefreshTokenRepository;
import com.fightverse.identity.infrastructure.persistence.UserRepository;
import com.fightverse.identity.infrastructure.security.JwtTokenService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  /** BCrypt ignores anything past the first 72 bytes, so longer secrets would alias. */
  private static final int BCRYPT_MAX_BYTES = 72;

  private final UserRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenService jwtTokenService;
  private final SecurityProperties securityProperties;
  private final Clock clock;
  private final String timingEqualizerHash;

  public AuthService(
      UserRepository users,
      RefreshTokenRepository refreshTokens,
      PasswordEncoder passwordEncoder,
      JwtTokenService jwtTokenService,
      SecurityProperties securityProperties,
      Clock clock) {
    this.users = users;
    this.refreshTokens = refreshTokens;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenService = jwtTokenService;
    this.securityProperties = securityProperties;
    this.clock = clock;
    this.timingEqualizerHash = passwordEncoder.encode(UUID.randomUUID().toString());
  }

  @Transactional
  public AuthSessionResponse register(
      String email, String rawPassword, String displayName, String userAgent) {

    String normalizedEmail = EmailNormalizer.normalize(email);
    if (users.existsByEmail(normalizedEmail)) {
      throw new ConflictException("An account with this email already exists.");
    }
    ensureBcryptCompatible(rawPassword);

    User user =
        users.save(User.register(normalizedEmail, passwordEncoder.encode(rawPassword), displayName));
    return AuthSessionResponse.of(user, issueTokens(user, userAgent));
  }

  @Transactional
  public AuthSessionResponse login(
      String email, String rawPassword, String userAgent) {

    User user = users.findByEmail(EmailNormalizer.normalize(email)).orElse(null);
    if (user == null) {
      // Hash anyway so a missing account cannot be distinguished from a wrong password by timing.
      passwordEncoder.matches(rawPassword, timingEqualizerHash);
      throw new BadCredentialsException("Invalid email or password.");
    }
    if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid email or password.");
    }
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new DisabledException("This account is not active.");
    }
    return AuthSessionResponse.of(user, issueTokens(user, userAgent));
  }

  @Transactional
  public TokenResponse refresh(String rawRefreshToken, String userAgent) {
    Instant now = clock.instant();
    RefreshToken token =
        refreshTokens
            .findByTokenHash(Secrets.sha256Hex(rawRefreshToken))
            .orElseThrow(AuthService::invalidRefreshToken);

    if (token.getRevokedAt() != null) {
      // A revoked token coming back means it leaked: drop every session for that user.
      int revoked = refreshTokens.revokeAllActiveForUser(token.getUser().getId(), now);
      log.warn(
          "Refresh token reuse detected for user {}; revoked {} active session(s)",
          token.getUser().getId(),
          revoked);
      throw invalidRefreshToken();
    }
    if (!token.getExpiresAt().isAfter(now)) {
      throw invalidRefreshToken();
    }

    token.revoke(now);
    return issueTokens(token.getUser(), userAgent);
  }

  @Transactional
  public void logout(UUID userId, String rawRefreshToken) {
    Instant now = clock.instant();
    refreshTokens
        .findByTokenHash(Secrets.sha256Hex(rawRefreshToken))
        .filter(token -> token.belongsTo(userId))
        .ifPresent(token -> token.revoke(now));
  }

  private TokenResponse issueTokens(User user, String userAgent) {
    JwtTokenService.IssuedAccessToken accessToken = jwtTokenService.issue(user);
    String rawRefreshToken = Secrets.newToken();
    Instant refreshExpiresAt = clock.instant().plus(securityProperties.refreshTokenTtl());

    refreshTokens.save(
        RefreshToken.issue(
            user, Secrets.sha256Hex(rawRefreshToken), refreshExpiresAt, userAgent));

    return new TokenResponse(
        accessToken.token(),
        TokenResponse.BEARER,
        accessToken.expiresAt(),
        rawRefreshToken,
        refreshExpiresAt);
  }

  private static void ensureBcryptCompatible(String rawPassword) {
    if (rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
      throw new InvalidRequestException("password must be at most 72 bytes when UTF-8 encoded.");
    }
  }

  private static BadCredentialsException invalidRefreshToken() {
    return new BadCredentialsException("The refresh token is invalid or has expired.");
  }
}
