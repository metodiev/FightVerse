package com.fightverse.catalog.domain;

import com.fightverse.common.error.InvalidRequestException;
import tools.jackson.databind.JsonNode;

/**
 * The technique-specific half of a technique definition. Keeping it in JSONB lets new disciplines
 * and analyzers be added without a schema migration; the shape is validated here and versioned via
 * {@code schemaVersion}.
 */
public record TechniqueDefinition(
    int schemaVersion,
    JsonNode movement,
    JsonNode requiredLandmarks,
    JsonNode evaluationCriteria,
    JsonNode scoringRules,
    JsonNode analyzerConfig) {

  public TechniqueDefinition {
    if (schemaVersion < 1) {
      throw new InvalidRequestException("definitionSchemaVersion must be at least 1.");
    }
    requireObject(movement, "movementDefinition");
    requireNonEmptyArray(requiredLandmarks, "requiredLandmarks");
    requireNonEmptyArray(evaluationCriteria, "evaluationCriteria");
    requireOptionalObject(scoringRules, "scoringRules");
    requireOptionalObject(analyzerConfig, "analyzerConfig");
  }

  private static void requireObject(JsonNode node, String field) {
    if (node == null || !node.isObject()) {
      throw new InvalidRequestException(field + " must be a JSON object.");
    }
  }

  private static void requireOptionalObject(JsonNode node, String field) {
    if (node != null && !node.isNull() && !node.isObject()) {
      throw new InvalidRequestException(field + " must be a JSON object when provided.");
    }
  }

  private static void requireNonEmptyArray(JsonNode node, String field) {
    if (node == null || !node.isArray() || node.isEmpty()) {
      throw new InvalidRequestException(field + " must be a non-empty JSON array.");
    }
  }
}
