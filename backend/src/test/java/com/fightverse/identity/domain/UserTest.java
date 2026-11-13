package com.fightverse.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void registerNormalizesEmailAndActivatesStudent() {
    User user = User.register("  Fighter@Example.COM ", "hash", "  Miro  ");

    assertThat(user.getEmail()).isEqualTo("fighter@example.com");
    assertThat(user.getPasswordHash()).isEqualTo("hash");
    assertThat(user.getDisplayName()).isEqualTo("Miro");
    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(user.getRoles()).containsExactly(Role.STUDENT);
    assertThat(user.getEmailVerifiedAt()).isNull();
  }

  @Test
  void grantRoleAddsRoleWithoutLosingExistingOnes() {
    User user = User.register("fighter@example.com", "hash", "Miro");

    user.grantRole(Role.INSTRUCTOR);
    user.grantRole(Role.ADMIN);

    assertThat(user.hasRole(Role.STUDENT)).isTrue();
    assertThat(user.hasRole(Role.INSTRUCTOR)).isTrue();
    assertThat(user.hasRole(Role.ADMIN)).isTrue();
  }

  @Test
  void grantRoleIsIdempotent() {
    User user = User.register("fighter@example.com", "hash", "Miro");

    user.grantRole(Role.ADMIN);
    user.grantRole(Role.ADMIN);

    assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.STUDENT, Role.ADMIN);
  }

  @Test
  void hasRoleIsFalseForUngrantedRole() {
    User user = User.register("fighter@example.com", "hash", "Miro");

    assertThat(user.hasRole(Role.ADMIN)).isFalse();
    assertThat(user.hasRole(Role.INSTRUCTOR)).isFalse();
  }

  @Test
  void getRolesReturnsUnmodifiableView() {
    User user = User.register("fighter@example.com", "hash", "Miro");

    assertThatThrownBy(() -> user.getRoles().add(Role.ADMIN))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
