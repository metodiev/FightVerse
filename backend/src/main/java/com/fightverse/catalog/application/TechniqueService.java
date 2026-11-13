package com.fightverse.catalog.application;

import com.fightverse.catalog.api.dto.CreateTechniqueRequest;
import com.fightverse.catalog.api.dto.TechniqueResponse;
import com.fightverse.catalog.api.dto.TechniqueSummaryResponse;
import com.fightverse.catalog.domain.Discipline;
import com.fightverse.catalog.domain.Technique;
import com.fightverse.catalog.domain.TechniqueDefinition;
import com.fightverse.catalog.domain.TechniqueDifficulty;
import com.fightverse.catalog.domain.TechniqueStatus;
import com.fightverse.catalog.infrastructure.DisciplineRepository;
import com.fightverse.catalog.infrastructure.TechniqueRepository;
import com.fightverse.common.api.PageResponse;
import com.fightverse.common.api.Paging;
import com.fightverse.common.error.ConflictException;
import com.fightverse.common.error.ResourceNotFoundException;
import com.fightverse.common.util.Text;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TechniqueService {

  private static final Sort DISPLAY_ORDER = Sort.by(Sort.Order.asc("name"));

  private final TechniqueRepository techniques;
  private final DisciplineRepository disciplines;

  public TechniqueService(
      TechniqueRepository techniques, DisciplineRepository disciplines) {
    this.techniques = techniques;
    this.disciplines = disciplines;
  }

  public PageResponse<TechniqueSummaryResponse> list(
      String disciplineCode, TechniqueDifficulty difficulty, int page, int size) {

    Pageable pageable = Paging.of(page, size, DISPLAY_ORDER);
    String code = Text.trimToNull(disciplineCode);
    code = code == null ? null : code.toLowerCase(Locale.ROOT);

    Page<Technique> result;
    if (code == null && difficulty == null) {
      result = techniques.findByStatus(TechniqueStatus.PUBLISHED, pageable);
    } else if (difficulty == null) {
      result = techniques.findByStatusAndDisciplineCode(TechniqueStatus.PUBLISHED, code, pageable);
    } else if (code == null) {
      result = techniques.findByStatusAndDifficulty(TechniqueStatus.PUBLISHED, difficulty, pageable);
    } else {
      result =
          techniques.findByStatusAndDisciplineCodeAndDifficulty(
              TechniqueStatus.PUBLISHED, code, difficulty, pageable);
    }
    return PageResponse.from(result, TechniqueSummaryResponse::from);
  }

  public TechniqueResponse get(UUID id) {
    return techniques
        .findByIdAndStatus(id, TechniqueStatus.PUBLISHED)
        .map(TechniqueResponse::from)
        .orElseThrow(
            () -> new ResourceNotFoundException("Technique '%s' was not found.".formatted(id)));
  }

  @Transactional
  public TechniqueResponse create(String disciplineCode, CreateTechniqueRequest request) {
    Discipline discipline =
        disciplines
            .findByCode(disciplineCode.trim().toLowerCase(Locale.ROOT))
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Discipline '%s' was not found.".formatted(disciplineCode)));

    String code = request.code().trim().toLowerCase(Locale.ROOT);
    if (techniques.existsByDisciplineIdAndCode(discipline.getId(), code)) {
      throw new ConflictException(
          "A technique with code '%s' already exists in discipline '%s'."
              .formatted(code, discipline.getCode()));
    }

    int schemaVersion =
        request.definitionSchemaVersion() == null ? 1 : request.definitionSchemaVersion();
    TechniqueDefinition definition =
        new TechniqueDefinition(
            schemaVersion,
            request.movementDefinition(),
            request.requiredLandmarks(),
            request.evaluationCriteria(),
            request.scoringRules(),
            request.analyzerConfig());

    Technique technique =
        techniques.save(
            Technique.create(
                discipline,
                code,
                request.name().trim(),
                Text.trimToNull(request.description()),
                request.difficulty(),
                definition));
    return TechniqueResponse.from(technique);
  }
}
