package com.fightverse.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class PagingTest {

  private static final Sort NAME_ASC = Sort.by(Sort.Order.asc("name"));

  @Test
  void keepsValidPageSizeAndSort() {
    Pageable pageable = Paging.of(2, 25, NAME_ASC);

    assertThat(pageable.getPageNumber()).isEqualTo(2);
    assertThat(pageable.getPageSize()).isEqualTo(25);
    assertThat(pageable.getSort()).isEqualTo(NAME_ASC);
  }

  @Test
  void clampsNegativePageToZero() {
    assertThat(Paging.of(-5, 20, NAME_ASC).getPageNumber()).isZero();
  }

  @Test
  void clampsSizeLowerBoundToOne() {
    assertThat(Paging.of(0, 0, NAME_ASC).getPageSize()).isEqualTo(1);
    assertThat(Paging.of(0, -10, NAME_ASC).getPageSize()).isEqualTo(1);
  }

  @Test
  void clampsSizeUpperBoundToMaxPageSize() {
    assertThat(Paging.of(0, Paging.MAX_PAGE_SIZE + 1, NAME_ASC).getPageSize())
        .isEqualTo(Paging.MAX_PAGE_SIZE);
    assertThat(Paging.of(0, Integer.MAX_VALUE, NAME_ASC).getPageSize())
        .isEqualTo(Paging.MAX_PAGE_SIZE);
  }

  @Test
  void keepsSizeAtExactBounds() {
    assertThat(Paging.of(0, 1, NAME_ASC).getPageSize()).isEqualTo(1);
    assertThat(Paging.of(0, Paging.MAX_PAGE_SIZE, NAME_ASC).getPageSize())
        .isEqualTo(Paging.MAX_PAGE_SIZE);
  }
}
