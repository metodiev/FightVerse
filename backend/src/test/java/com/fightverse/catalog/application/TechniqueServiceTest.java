package com.fightverse.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fightverse.catalog.api.dto.CreateTechniqueRequest;
import com.fightverse.catalog.api.dto.TechniqueResponse;
import com.fightverse.catalog.api.dto.TechniqueSummaryResponse;
import com.fightverse.catalog.domain.Discipline;
import com.fightverse.catalog.domain.Technique;
import com.fightverse.catalog.domain.TechniqueDifficulty;
import com.fightverse.catalog.domain.TechniqueDefinition;
import com.fightverse.catalog.domain.TechniqueStatus;
import com.fightverse.catalog.infrastructure.DisciplineRepository;
import com.fightverse.catalog.infrastructure.TechniqueRepository;
import com.fightverse.common.api.PageResponse;
import com.fightverse.common.error.ConflictException;
import com.fightverse.common.error.ResourceNotFoundException;
import com.fightverse.support.TestEntities;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

class TechniqueServiceTest {

  private static final UUID DISCIPLINE_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID TECHNIQUE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final JsonNode MOVEMENT =
      JsonNodeFactory.instance.objectNode().put("kind", "jab");
  private static final JsonNode LANDMARKS = JsonNodeFactory.instance.arrayNode().add("shoulder");
  private static final JsonNode CRITERIA = JsonNodeFactory.instance.arrayNode().add("extension");

  private final TechniqueRepository techniques = mock(TechniqueRepository.class);
  private final DisciplineRepository disciplines = mock(DisciplineRepository.class);
  private final TechniqueService service = new TechniqueService(techniques, disciplines);

  private final Discipline boxing =
      TestEntities.assignId(Discipline.create("boxing", "Boxing", "Sweet science", 0),
          DISCIPLINE_ID);

  @Test
  void listWithoutFiltersQueriesPublishedTechniques() {
    when(techniques.findByStatus(eq(TechniqueStatus.PUBLISHED), any(Pageable.class)))
        .thenReturn(pageOf(technique()));

    PageResponse<TechniqueSummaryResponse> response = service.list(null, null, 0, 20);

    assertThat(response.content()).singleElement().satisfies(item -> {
      assertThat(item.code()).isEqualTo("left-jab");
      assertThat(item.disciplineCode()).isEqualTo("boxing");
      assertThat(item.difficulty()).isEqualTo(TechniqueDifficulty.BEGINNER);
    });

    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(techniques).findByStatus(eq(TechniqueStatus.PUBLISHED), pageable.capture());
    assertThat(pageable.getValue().getPageNumber()).isZero();
    assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Order.asc("name")));
  }

  @Test
  void listNormalizesDisciplineCode() {
    when(techniques.findByStatusAndDisciplineCode(
            eq(TechniqueStatus.PUBLISHED), eq("boxing"), any(Pageable.class)))
        .thenReturn(pageOf(technique()));

    service.list("  Boxing  ", null, 0, 20);

    verify(techniques)
        .findByStatusAndDisciplineCode(
            eq(TechniqueStatus.PUBLISHED), eq("boxing"), any(Pageable.class));
  }

  @Test
  void listTreatsBlankDisciplineAsAbsent() {
    when(techniques.findByStatus(eq(TechniqueStatus.PUBLISHED), any(Pageable.class)))
        .thenReturn(pageOf(technique()));

    service.list("   ", null, 0, 20);

    verify(techniques).findByStatus(eq(TechniqueStatus.PUBLISHED), any(Pageable.class));
  }

  @Test
  void listFiltersByDifficultyOnly() {
    when(techniques.findByStatusAndDifficulty(
            eq(TechniqueStatus.PUBLISHED), eq(TechniqueDifficulty.ADVANCED), any(Pageable.class)))
        .thenReturn(pageOf(technique()));

    service.list(null, TechniqueDifficulty.ADVANCED, 0, 20);

    verify(techniques)
        .findByStatusAndDifficulty(
            eq(TechniqueStatus.PUBLISHED), eq(TechniqueDifficulty.ADVANCED), any(Pageable.class));
  }

  @Test
  void listFiltersByDisciplineAndDifficulty() {
    when(techniques.findByStatusAndDisciplineCodeAndDifficulty(
            eq(TechniqueStatus.PUBLISHED),
            eq("boxing"),
            eq(TechniqueDifficulty.ADVANCED),
            any(Pageable.class)))
        .thenReturn(pageOf(technique()));

    service.list("Boxing", TechniqueDifficulty.ADVANCED, 0, 20);

    verify(techniques)
        .findByStatusAndDisciplineCodeAndDifficulty(
            eq(TechniqueStatus.PUBLISHED),
            eq("boxing"),
            eq(TechniqueDifficulty.ADVANCED),
            any(Pageable.class));
  }

  @Test
  void getReturnsPublishedTechnique() {
    when(techniques.findByIdAndStatus(TECHNIQUE_ID, TechniqueStatus.PUBLISHED))
        .thenReturn(Optional.of(technique()));

    TechniqueResponse response = service.get(TECHNIQUE_ID);

    assertThat(response.id()).isEqualTo(TECHNIQUE_ID);
    assertThat(response.code()).isEqualTo("left-jab");
    assertThat(response.status()).isEqualTo(TechniqueStatus.PUBLISHED);
  }

  @Test
  void getFailsWhenTechniqueIsNotPublished() {
    when(techniques.findByIdAndStatus(TECHNIQUE_ID, TechniqueStatus.PUBLISHED))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(TECHNIQUE_ID))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining(TECHNIQUE_ID.toString());
  }

  @Test
  void createRejectsUnknownDiscipline() {
    when(disciplines.findByCode("nope")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create("nope", request(null)))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Discipline 'nope' was not found.");
  }

  @Test
  void createRejectsDuplicateTechniqueCodeWithinDiscipline() {
    when(disciplines.findByCode("boxing")).thenReturn(Optional.of(boxing));
    when(techniques.existsByDisciplineIdAndCode(DISCIPLINE_ID, "left-jab")).thenReturn(true);

    assertThatThrownBy(() -> service.create("boxing", request(null)))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("left-jab");
  }

  @Test
  void createNormalizesInputAndDefaultsSchemaVersion() {
    when(disciplines.findByCode("boxing")).thenReturn(Optional.of(boxing));
    when(techniques.existsByDisciplineIdAndCode(DISCIPLINE_ID, "left-jab")).thenReturn(false);
    when(techniques.save(any(Technique.class)))
        .thenAnswer(invocation -> TestEntities.assignId(invocation.getArgument(0), TECHNIQUE_ID));

    TechniqueResponse response = service.create("  Boxing ", request(null));

    assertThat(response.id()).isEqualTo(TECHNIQUE_ID);
    assertThat(response.code()).isEqualTo("left-jab");
    assertThat(response.name()).isEqualTo("Left Jab");
    assertThat(response.description()).isNull();
    assertThat(response.definitionSchemaVersion()).isEqualTo(1);
    assertThat(response.status()).isEqualTo(TechniqueStatus.PUBLISHED);
    assertThat(response.disciplineCode()).isEqualTo("boxing");
  }

  @Test
  void createKeepsExplicitSchemaVersion() {
    when(disciplines.findByCode("boxing")).thenReturn(Optional.of(boxing));
    when(techniques.existsByDisciplineIdAndCode(DISCIPLINE_ID, "left-jab")).thenReturn(false);
    when(techniques.save(any(Technique.class)))
        .thenAnswer(invocation -> TestEntities.assignId(invocation.getArgument(0), TECHNIQUE_ID));

    TechniqueResponse response = service.create("boxing", request(3));

    assertThat(response.definitionSchemaVersion()).isEqualTo(3);
  }

  private static CreateTechniqueRequest request(Integer schemaVersion) {
    return new CreateTechniqueRequest(
        " Left-Jab ",
        "  Left Jab  ",
        "   ",
        TechniqueDifficulty.BEGINNER,
        schemaVersion,
        MOVEMENT,
        LANDMARKS,
        CRITERIA,
        null,
        null);
  }

  private Technique technique() {
    return TestEntities.assignId(
        Technique.create(
            boxing,
            "left-jab",
            "Left Jab",
            null,
            TechniqueDifficulty.BEGINNER,
            new TechniqueDefinition(
                1, MOVEMENT, LANDMARKS, CRITERIA, null, null)),        TECHNIQUE_ID);
  }

  private static Page<Technique> pageOf(Technique technique) {
    return new PageImpl<>(List.of(technique), PageRequest.of(0, 20), 1);
  }
}
