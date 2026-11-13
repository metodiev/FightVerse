package com.fightverse.catalog.api.dto;

import com.fightverse.catalog.domain.Technique;
import com.fightverse.catalog.domain.TechniqueDifficulty;
import java.util.UUID;

public record TechniqueSummaryResponse(
    UUID id,
    String disciplineCode,
    String code,
    String name,
    String description,
    TechniqueDifficulty difficulty) {

  public static TechniqueSummaryResponse from(Technique technique) {
    return new TechniqueSummaryResponse(
        technique.getId(),
        technique.getDiscipline().getCode(),
        technique.getCode(),
        technique.getName(),
        technique.getDescription(),
        technique.getDifficulty());
  }
}
