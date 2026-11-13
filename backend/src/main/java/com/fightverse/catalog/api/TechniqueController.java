package com.fightverse.catalog.api;

import com.fightverse.catalog.api.dto.TechniqueResponse;
import com.fightverse.catalog.api.dto.TechniqueSummaryResponse;
import com.fightverse.catalog.application.TechniqueService;
import com.fightverse.catalog.domain.TechniqueDifficulty;
import com.fightverse.common.api.PageResponse;
import com.fightverse.common.api.Paging;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/techniques")
@Validated
public class TechniqueController {

  private final TechniqueService techniqueService;

  public TechniqueController(TechniqueService techniqueService) {
    this.techniqueService = techniqueService;
  }

  @GetMapping
  public PageResponse<TechniqueSummaryResponse> list(
      @RequestParam(required = false) @Size(max = 32) String discipline,
      @RequestParam(required = false) @Size(max = 32) String difficulty,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(Paging.MAX_PAGE_SIZE) int size) {

    return techniqueService.list(discipline, TechniqueDifficulty.parse(difficulty), page, size);
  }

  @GetMapping("/{id}")
  public TechniqueResponse get(@PathVariable UUID id) {
    return techniqueService.get(id);
  }
}
