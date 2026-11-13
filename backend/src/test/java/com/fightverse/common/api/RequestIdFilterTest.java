package com.fightverse.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

  private final RequestIdFilter filter = new RequestIdFilter();

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void generatesIdWhenHeaderMissing() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/techniques");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> seenInsideChain = new AtomicReference<>();

    filter.doFilter(
        request, response, (req, res) -> seenInsideChain.set(MDC.get(RequestIdFilter.MDC_KEY)));

    String header = response.getHeader(RequestIdFilter.HEADER);
    assertThat(header).isNotBlank();
    assertThat(seenInsideChain.get()).isEqualTo(header);
    assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
  }

  @Test
  void echoesClientSuppliedId() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/techniques");
    request.addHeader(RequestIdFilter.HEADER, "client-correlation-id");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> {});

    assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("client-correlation-id");
  }

  @Test
  void regeneratesBlankId() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/techniques");
    request.addHeader(RequestIdFilter.HEADER, "   ");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> {});

    assertThat(response.getHeader(RequestIdFilter.HEADER)).isNotBlank().doesNotContain(" ");
  }

  @Test
  void regeneratesOverlyLongId() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/techniques");
    request.addHeader(RequestIdFilter.HEADER, "x".repeat(65));
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> {});

    assertThat(response.getHeader(RequestIdFilter.HEADER)).hasSizeLessThanOrEqualTo(64);
    assertThat(response.getHeader(RequestIdFilter.HEADER)).isNotEqualTo("x".repeat(65));
  }

  @Test
  void acceptsIdAtMaximumLength() throws Exception {
    String maxLength = "x".repeat(64);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/techniques");
    request.addHeader(RequestIdFilter.HEADER, maxLength);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> {});

    assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(maxLength);
  }

  @Test
  void clearsMdcEvenWhenChainFails() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/techniques");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThatThrownBy(
            () ->
                filter.doFilter(
                    request,
                    response,
                    (req, res) -> {
                      throw new ServletException("boom");
                    }))
        .isInstanceOf(ServletException.class);

    assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
  }
}
