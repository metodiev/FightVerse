package com.fightverse.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "email is required")
        @Email(message = "must be a valid email address")
        @Size(max = 320, message = "must be at most 320 characters")
        String email,
    @NotBlank(message = "password is required")
        @Size(min = 12, max = 64, message = "must be between 12 and 64 characters")
        String password,
    @NotBlank(message = "displayName is required")
        @Size(max = 120, message = "must be at most 120 characters")
        String displayName) {}
