package com.fightverse.identity.api.dto;

import com.fightverse.identity.domain.User;

public record AuthSessionResponse(UserResponse user, TokenResponse tokens) {

  public static AuthSessionResponse of(User user, TokenResponse tokens) {
    return new AuthSessionResponse(UserResponse.from(user), tokens);
  }
}
