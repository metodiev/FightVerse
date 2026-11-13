package com.fightverse.catalog.domain;

import com.fightverse.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "techniques")
public class Technique extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "discipline_id", nullable = false)
  private Discipline discipline;

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(length = 2000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private TechniqueDifficulty difficulty;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private TechniqueStatus status;

  @Column(name = "definition_schema_version", nullable = false)
  private int definitionSchemaVersion;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "movement_definition", nullable = false, columnDefinition = "jsonb")
  private JsonNode movementDefinition;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "required_landmarks", nullable = false, columnDefinition = "jsonb")
  private JsonNode requiredLandmarks;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "evaluation_criteria", nullable = false, columnDefinition = "jsonb")
  private JsonNode evaluationCriteria;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "scoring_rules", columnDefinition = "jsonb")
  private JsonNode scoringRules;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "analyzer_config", columnDefinition = "jsonb")
  private JsonNode analyzerConfig;

  @Version
  @Column(nullable = false)
  private long version;

  protected Technique() {}

  public static Technique create(
      Discipline discipline,
      String code,
      String name,
      String description,
      TechniqueDifficulty difficulty,
      TechniqueDefinition definition) {

    Technique technique = new Technique();
    technique.discipline = discipline;
    technique.code = code;
    technique.name = name;
    technique.description = description;
    technique.difficulty = difficulty;
    technique.status = TechniqueStatus.PUBLISHED;
    technique.definitionSchemaVersion = definition.schemaVersion();
    technique.movementDefinition = definition.movement();
    technique.requiredLandmarks = definition.requiredLandmarks();
    technique.evaluationCriteria = definition.evaluationCriteria();
    technique.scoringRules = definition.scoringRules();
    technique.analyzerConfig = definition.analyzerConfig();
    return technique;
  }

  public UUID getId() {
    return id;
  }

  public Discipline getDiscipline() {
    return discipline;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public TechniqueDifficulty getDifficulty() {
    return difficulty;
  }

  public TechniqueStatus getStatus() {
    return status;
  }

  public int getDefinitionSchemaVersion() {
    return definitionSchemaVersion;
  }

  public JsonNode getMovementDefinition() {
    return movementDefinition;
  }

  public JsonNode getRequiredLandmarks() {
    return requiredLandmarks;
  }

  public JsonNode getEvaluationCriteria() {
    return evaluationCriteria;
  }

  public JsonNode getScoringRules() {
    return scoringRules;
  }

  public JsonNode getAnalyzerConfig() {
    return analyzerConfig;
  }

  public long getVersion() {
    return version;
  }
}
