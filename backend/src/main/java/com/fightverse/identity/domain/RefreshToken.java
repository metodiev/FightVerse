package com.fightverse.identity.domain;

import com.fightverse.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** SHA-256 of the token handed to the client. The raw value is never stored. */
  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "user_agent", length = 512)
  private String userAgent;

  protected RefreshToken() {}

  public static RefreshToken issue(
      User user, String tokenHash, Instant expiresAt, String userAgent) {
    RefreshToken token = new RefreshToken();
    token.user = user;
    token.tokenHash = tokenHash;
    token.expiresAt = expiresAt;
    token.userAgent = truncate(userAgent);
    return token;
  }

  public void revoke(Instant now) {
    if (revokedAt == null) {
      revokedAt = now;
    }
  }

  public boolean isActive(Instant now) {
    return revokedAt == null && expiresAt.isAfter(now);
  }

  public boolean belongsTo(UUID userId) {
    return user.getId().equals(userId);
  }

  private static String truncate(String value) {
    if (value == null) {
      return null;
    }
    return value.length() <= 512 ? value : value.substring(0, 512);
  }

  public UUID getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public String getUserAgent() {
    return userAgent;
  }
}
