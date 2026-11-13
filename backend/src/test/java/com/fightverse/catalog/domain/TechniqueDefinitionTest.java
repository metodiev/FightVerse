package com.fightverse.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fightverse.common.error.InvalidRequestException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class TechniqueDefinitionTest {

  private static final ObjectNode MOVEMENT = objectNode("type", "jab");
  private static final ArrayNode LANDMARKS = stringArray("shoulder", "elbow");
  private static final ArrayNode CRITERIA = stringArray("extension", "retraction");

  @Test
  void acceptsMinimalValidDefinition() {
    TechniqueDefinition definition =
        new TechniqueDefinition(1, MOVEMENT, LANDMARKS, CRITERIA, null, null);

    assertThat(definition.schemaVersion()).isEqualTo(1);
    assertThat(definition.movement()).isSameAs(MOVEMENT);
    assertThat(definition.requiredLandmarks()).isSameAs(LANDMARKS);
    assertThat(definition.evaluationCriteria()).isSameAs(CRITERIA);
    assertThat(definition.scoringRules()).isNull();
    assertThat(definition.analyzerConfig()).isNull();
  }

  @Test
  void acceptsOptionalObjectsWhenProvided() {
    ObjectNode scoring = objectNode("points", "3");

    TechniqueDefinition definition =
        new TechniqueDefinition(2, MOVEMENT, LANDMARKS, CRITERIA, scoring, objectNode("fps", "30"));

    assertThat(definition.schemaVersion()).isEqualTo(2);
    assertThat(definition.scoringRules()).isSameAs(scoring);
  }

  @Test
  void rejectsSchemaVersionBelowOne() {
    assertThatThrownBy(() -> new TechniqueDefinition(0, MOVEMENT, LANDMARKS, CRITERIA, null, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("definitionSchemaVersion");
  }

  @Test
  void rejectsMissingMovement() {
    assertThatThrownBy(() -> new TechniqueDefinition(1, null, LANDMARKS, CRITERIA, null, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("movementDefinition");
  }

  @Test
  void rejectsNonObjectMovement() {
    JsonNode notAnObject = JsonNodeFactory.instance.numberNode(1);

    assertThatThrownBy(
            () -> new TechniqueDefinition(1, notAnObject, LANDMARKS, CRITERIA, null, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("movementDefinition");
  }

  @Test
  void rejectsMissingLandmarks() {
    assertThatThrownBy(() -> new TechniqueDefinition(1, MOVEMENT, null, CRITERIA, null, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("requiredLandmarks");
  }

  @Test
  void rejectsEmptyLandmarks() {
    assertThatThrownBy(
            () ->
                new TechniqueDefinition(
                    1, MOVEMENT, JsonNodeFactory.instance.arrayNode(), CRITERIA, null, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("requiredLandmarks");
  }

  @Test
  void rejectsNonArrayEvaluationCriteria() {
    assertThatThrownBy(
            () -> new TechniqueDefinition(1, MOVEMENT, LANDMARKS, null, null, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("evaluationCriteria");

    JsonNode notAnArray = JsonNodeFactory.instance.numberNode(1);
    assertThatThrownBy(
            () -> new TechniqueDefinition(1, MOVEMENT, LANDMARKS, notAnArray, null, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("evaluationCriteria");
  }

  @Test
  void rejectsNonObjectOptionalFields() {
    JsonNode number = JsonNodeFactory.instance.numberNode(1);

    assertThatThrownBy(
            () -> new TechniqueDefinition(1, MOVEMENT, LANDMARKS, CRITERIA, number, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("scoringRules");

    assertThatThrownBy(
            () -> new TechniqueDefinition(1, MOVEMENT, LANDMARKS, CRITERIA, null, number))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("analyzerConfig");
  }

  private static ObjectNode objectNode(String key, String value) {
    return JsonNodeFactory.instance.objectNode().put(key, value);
  }

  private static ArrayNode stringArray(String first, String second) {
    return JsonNodeFactory.instance.arrayNode().add(first).add(second);
  }
}
