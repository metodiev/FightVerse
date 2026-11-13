package com.fightverse.catalog.api.dto;

import com.fightverse.catalog.domain.Technique;
import com.fightverse.catalog.domain.TechniqueDifficulty;
import com.fightverse.catalog.domain.TechniqueStatus;
import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record TechniqueResponse(
    UUID id,
    String disciplineCode,
    String code,
    String name,
    String description,
    TechniqueDifficulty difficulty,
    TechniqueStatus status,
    int definitionSchemaVersion,
    JsonNode movementDefinition,
    JsonNode requiredLandmarks,
    JsonNode evaluationCriteria,
    JsonNode scoringRules,
    JsonNode analyzerConfig,
    long version,
    Instant createdAt,
    Instant updatedAt) {

  public static TechniqueResponse from(Technique technique) {
    return new TechniqueResponse(
        technique.getId(),
        technique.getDiscipline().getCode(),
        technique.getCode(),
        technique.getName(),
        technique.getDescription(),
        technique.getDifficulty(),
        technique.getStatus(),
        technique.getDefinitionSchemaVersion(),
        technique.getMovementDefinition(),
        technique.getRequiredLandmarks(),
        technique.getEvaluationCriteria(),
        technique.getScoringRules(),
        technique.getAnalyzerConfig(),
        technique.getVersion(),
        technique.getCreatedAt(),
        technique.getUpdatedAt());
  }
}
