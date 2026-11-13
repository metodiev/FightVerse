package com.fightverse.catalog.api;

import com.fightverse.catalog.api.dto.CreateDisciplineRequest;
import com.fightverse.catalog.api.dto.CreateTechniqueRequest;
import com.fightverse.catalog.api.dto.DisciplineResponse;
import com.fightverse.catalog.api.dto.TechniqueResponse;
import com.fightverse.catalog.application.DisciplineService;
import com.fightverse.catalog.application.TechniqueService;
import com.fightverse.common.api.PageResponse;
import com.fightverse.common.api.Paging;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/disciplines")
@Validated
public class DisciplineController {

  private final DisciplineService disciplineService;
  private final TechniqueService techniqueService;

  public DisciplineController(
      DisciplineService disciplineService, TechniqueService techniqueService) {
    this.disciplineService = disciplineService;
    this.techniqueService = techniqueService;
  }

  @GetMapping
  public PageResponse<DisciplineResponse> list(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(Paging.MAX_PAGE_SIZE) int size) {

    return disciplineService.list(page, size);
  }

  @GetMapping("/{code}")
  public DisciplineResponse get(@PathVariable String code) {
    return disciplineService.getByCode(code);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public DisciplineResponse create(@Valid @RequestBody CreateDisciplineRequest request) {
    return disciplineService.create(request);
  }

  @PostMapping("/{code}/techniques")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public TechniqueResponse createTechnique(
      @PathVariable String code, @Valid @RequestBody CreateTechniqueRequest request) {

    return techniqueService.create(code, request);
  }
}
