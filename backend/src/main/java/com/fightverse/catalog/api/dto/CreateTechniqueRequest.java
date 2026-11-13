package com.fightverse.catalog.api.dto;

import com.fightverse.catalog.domain.TechniqueDifficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

public record CreateTechniqueRequest(
    @NotBlank(message = "code is required")
        @Size(max = 64, message = "must be at most 64 characters")
        @Pattern(
            regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
            message = "must be lowercase kebab-case, for example 'left-jab'")
        String code,
    @NotBlank(message = "name is required")
        @Size(max = 120, message = "must be at most 120 characters")
        String name,
    @Size(max = 2000, message = "must be at most 2000 characters") String description,
    @NotNull(message = "difficulty is required") TechniqueDifficulty difficulty,
    @Min(value = 1, message = "must be at least 1")
        @Max(value = 1000, message = "must be at most 1000")
        Integer definitionSchemaVersion,
    @NotNull(message = "movementDefinition is required") JsonNode movementDefinition,
    @NotNull(message = "requiredLandmarks is required") JsonNode requiredLandmarks,
    @NotNull(message = "evaluationCriteria is required") JsonNode evaluationCriteria,
    JsonNode scoringRules,
    JsonNode analyzerConfig) {}
