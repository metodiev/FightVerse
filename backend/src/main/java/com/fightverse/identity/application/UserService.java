package com.fightverse.identity.application;

import com.fightverse.common.error.ResourceNotFoundException;
import com.fightverse.identity.domain.User;
import com.fightverse.identity.infrastructure.persistence.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository users;

  public UserService(UserRepository users) {
    this.users = users;
  }

  public User getById(UUID id) {
    return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found."));
  }
}
