package com.fightverse.catalog.application;

import com.fightverse.catalog.api.dto.CreateDisciplineRequest;
import com.fightverse.catalog.api.dto.DisciplineResponse;
import com.fightverse.catalog.domain.Discipline;
import com.fightverse.catalog.infrastructure.DisciplineRepository;
import com.fightverse.common.api.PageResponse;
import com.fightverse.common.api.Paging;
import com.fightverse.common.error.ConflictException;
import com.fightverse.common.error.ResourceNotFoundException;
import com.fightverse.common.util.Text;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DisciplineService {

  private static final Sort DISPLAY_ORDER =
      Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("name"));

  private final DisciplineRepository disciplines;

  public DisciplineService(DisciplineRepository disciplines) {
    this.disciplines = disciplines;
  }

  public PageResponse<DisciplineResponse> list(int page, int size) {
    return PageResponse.from(
        disciplines.findByActiveTrue(Paging.of(page, size, DISPLAY_ORDER)), DisciplineResponse::from);
  }

  public DisciplineResponse getByCode(String code) {
    return disciplines
        .findByCodeAndActiveTrue(normalizeCode(code))
        .map(DisciplineResponse::from)
        .orElseThrow(() -> notFound(code));
  }

  @Transactional
  public DisciplineResponse create(CreateDisciplineRequest request) {
    String code = normalizeCode(request.code());
    if (disciplines.existsByCode(code)) {
      throw new ConflictException("A discipline with code '%s' already exists.".formatted(code));
    }

    int sortOrder = request.sortOrder() == null ? 0 : request.sortOrder();
    Discipline discipline =
        disciplines.save(
            Discipline.create(
                code,
                request.name().trim(),
                Text.trimToNull(request.description()),
                sortOrder));
    return DisciplineResponse.from(discipline);
  }

  private static String normalizeCode(String code) {
    return code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
  }

  private static ResourceNotFoundException notFound(String code) {
    return new ResourceNotFoundException("Discipline '%s' was not found.".formatted(code));
  }
}
