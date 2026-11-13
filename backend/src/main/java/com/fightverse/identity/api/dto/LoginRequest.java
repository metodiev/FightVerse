package com.fightverse.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "email is required")
        @Email(message = "must be a valid email address")
        @Size(max = 320, message = "must be at most 320 characters")
        String email,
    @NotBlank(message = "password is required") @Size(max = 128) String password) {}
