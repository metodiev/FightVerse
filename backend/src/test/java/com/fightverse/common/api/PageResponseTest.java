package com.fightverse.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

  private final Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 7);

  @Test
  void copiesPageMetadata() {
    PageResponse<String> response = PageResponse.from(page);

    assertThat(response.content()).containsExactly("a", "b");
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.size()).isEqualTo(2);
    assertThat(response.totalElements()).isEqualTo(7);
    assertThat(response.totalPages()).isEqualTo(4);
  }

  @Test
  void mapsContentWhenMapperGiven() {
    PageResponse<Integer> response = PageResponse.from(page, String::length);

    assertThat(response.content()).containsExactly(1, 1);
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.totalElements()).isEqualTo(7);
  }

  @Test
  void keepsEmptyContentForEmptyPage() {
    Page<String> empty = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

    PageResponse<String> response = PageResponse.from(empty);

    assertThat(response.content()).isEmpty();
    assertThat(response.totalElements()).isZero();
    assertThat(response.totalPages()).isZero();
  }
}
