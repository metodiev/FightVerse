package com.fightverse.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fightverse.catalog.api.dto.CreateDisciplineRequest;
import com.fightverse.catalog.api.dto.DisciplineResponse;
import com.fightverse.catalog.domain.Discipline;
import com.fightverse.catalog.infrastructure.DisciplineRepository;
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

class DisciplineServiceTest {

  private static final UUID DISCIPLINE_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");

  private final DisciplineRepository disciplines = mock(DisciplineRepository.class);
  private final DisciplineService service = new DisciplineService(disciplines);

  @Test
  void listQueriesActiveDisciplinesWithDisplayOrder() {
    when(disciplines.findByActiveTrue(any(Pageable.class))).thenReturn(pageOf(boxing()));

    PageResponse<DisciplineResponse> response = service.list(0, 20);

    assertThat(response.content()).singleElement()
        .satisfies(item -> assertThat(item.code()).isEqualTo("boxing"));

    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(disciplines).findByActiveTrue(pageable.capture());
    assertThat(pageable.getValue().getSort())
        .isEqualTo(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("name")));
  }

  @Test
  void getByCodeNormalizesInput() {
    when(disciplines.findByCodeAndActiveTrue("muay-thai")).thenReturn(Optional.of(boxing()));

    DisciplineResponse response = service.getByCode("  Muay-Thai  ");

    assertThat(response.code()).isEqualTo("boxing");
  }

  @Test
  void getByCodeFailsWithMessageEchoingSuppliedCode() {
    when(disciplines.findByCodeAndActiveTrue("unknown")).thenReturn(Optional.empty());

    // The lookup is normalized, but the error message echoes the caller's raw value.
    assertThatThrownBy(() -> service.getByCode(" UNKNOWN "))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Discipline ' UNKNOWN ' was not found.");
  }

  @Test
  void createNormalizesCodeAndNameAndDefaultsSortOrder() {
    when(disciplines.existsByCode("muay-thai")).thenReturn(false);
    when(disciplines.save(any(Discipline.class)))
        .thenAnswer(invocation -> TestEntities.assignId(invocation.getArgument(0), DISCIPLINE_ID));

    DisciplineResponse response =
        service.create(new CreateDisciplineRequest(" Muay-Thai ", "  Muay Thai  ", "   ", null));

    assertThat(response.id()).isEqualTo(DISCIPLINE_ID);
    assertThat(response.code()).isEqualTo("muay-thai");
    assertThat(response.name()).isEqualTo("Muay Thai");
    assertThat(response.description()).isNull();
    assertThat(response.sortOrder()).isZero();
  }

  @Test
  void createKeepsExplicitSortOrderAndDescription() {
    when(disciplines.existsByCode("muay-thai")).thenReturn(false);
    when(disciplines.save(any(Discipline.class)))
        .thenAnswer(invocation -> TestEntities.assignId(invocation.getArgument(0), DISCIPLINE_ID));

    DisciplineResponse response =
        service.create(
            new CreateDisciplineRequest("muay-thai", "Muay Thai", "  Art of eight limbs ", 7));

    assertThat(response.sortOrder()).isEqualTo(7);
    assertThat(response.description()).isEqualTo("Art of eight limbs");
  }

  @Test
  void createRejectsDuplicateCode() {
    when(disciplines.existsByCode("boxing")).thenReturn(true);

    assertThatThrownBy(
            () -> service.create(new CreateDisciplineRequest("boxing", "Boxing", null, null)))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("boxing");
  }

  private static Discipline boxing() {
    return TestEntities.assignId(
        Discipline.create("boxing", "Boxing", "Sweet science", 0), DISCIPLINE_ID);
  }

  private static Page<Discipline> pageOf(Discipline discipline) {
    return new PageImpl<>(List.of(discipline), PageRequest.of(0, 20), 1);
  }
}
