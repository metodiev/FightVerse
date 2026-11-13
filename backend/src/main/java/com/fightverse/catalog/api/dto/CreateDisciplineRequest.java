package com.fightverse.catalog.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDisciplineRequest(
    @NotBlank(message = "code is required")
        @Size(max = 32, message = "must be at most 32 characters")
        @Pattern(
            regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
            message = "must be lowercase kebab-case, for example 'muay-thai'")
        String code,
    @NotBlank(message = "name is required")
        @Size(max = 80, message = "must be at most 80 characters")
        String name,
    @Size(max = 2000, message = "must be at most 2000 characters") String description,
    @Min(value = 0, message = "must be zero or greater")
        @Max(value = 10_000, message = "must be at most 10000")
        Integer sortOrder) {}
