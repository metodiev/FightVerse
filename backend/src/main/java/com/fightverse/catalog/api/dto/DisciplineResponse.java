package com.fightverse.catalog.api.dto;

import com.fightverse.catalog.domain.Discipline;
import java.util.UUID;

public record DisciplineResponse(
    UUID id, String code, String name, String description, int sortOrder) {

  public static DisciplineResponse from(Discipline discipline) {
    return new DisciplineResponse(
        discipline.getId(),
        discipline.getCode(),
        discipline.getName(),
        discipline.getDescription(),
        discipline.getSortOrder());
  }
}
