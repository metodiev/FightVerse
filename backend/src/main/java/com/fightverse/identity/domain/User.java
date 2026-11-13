package com.fightverse.identity.domain;

import com.fightverse.common.persistence.AuditableEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 320)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Column(name = "display_name", nullable = false, length = 120)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private UserStatus status;

  @Column(name = "email_verified_at")
  private Instant emailVerifiedAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "role", nullable = false, length = 32)
  @Enumerated(EnumType.STRING)
  private Set<Role> roles = new HashSet<>();

  protected User() {}

  public static User register(String email, String passwordHash, String displayName) {
    User user = new User();
    user.email = EmailNormalizer.normalize(email);
    user.passwordHash = passwordHash;
    user.displayName = displayName.trim();
    user.status = UserStatus.ACTIVE;
    user.roles = EnumSet.of(Role.STUDENT);
    return user;
  }

  public void grantRole(Role role) {
    roles.add(role);
  }

  public boolean hasRole(Role role) {
    return roles.contains(role);
  }

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getDisplayName() {
    return displayName;
  }

  public UserStatus getStatus() {
    return status;
  }

  public Instant getEmailVerifiedAt() {
    return emailVerifiedAt;
  }

  public Set<Role> getRoles() {
    return Set.copyOf(roles);
  }
}
