package com.fightverse.identity.api.dto;

import com.fightverse.identity.domain.Role;
import com.fightverse.identity.domain.User;
import com.fightverse.identity.domain.UserStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String displayName,
    Set<Role> roles,
    UserStatus status,
    Instant createdAt) {

  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getDisplayName(),
        user.getRoles(),
        user.getStatus(),
        user.getCreatedAt());
  }
}
