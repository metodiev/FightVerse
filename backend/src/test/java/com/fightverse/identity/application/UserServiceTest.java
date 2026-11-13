package com.fightverse.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fightverse.common.error.ResourceNotFoundException;
import com.fightverse.identity.domain.User;
import com.fightverse.identity.infrastructure.persistence.UserRepository;
import com.fightverse.support.TestEntities;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserServiceTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  private final UserRepository users = mock(UserRepository.class);
  private final UserService userService = new UserService(users);

  @Test
  void getByIdReturnsMatchingUser() {
    User user = TestEntities.assignId(User.register("fighter@example.com", "hash", "Miro"), USER_ID);
    when(users.findById(USER_ID)).thenReturn(Optional.of(user));

    assertThat(userService.getById(USER_ID)).isSameAs(user);
  }

  @Test
  void getByIdFailsWhenUserIsMissing() {
    when(users.findById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getById(USER_ID))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("User not found.");
  }
}
