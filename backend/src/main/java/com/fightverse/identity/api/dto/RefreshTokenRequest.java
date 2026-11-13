package com.fightverse.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
    @NotBlank(message = "refreshToken is required")
        @Size(max = 512, message = "must be at most 512 characters")
        String refreshToken) {}
