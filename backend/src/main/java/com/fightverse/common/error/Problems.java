package com.fightverse.common.error;

import com.fightverse.common.api.RequestIdFilter;
import java.net.URI;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class Problems {

  private static final String TYPE_BASE = "https://api.fightverse.app/problems/";

  private Problems() {}

  public static ProblemDetail of(
      HttpStatus status, String code, String title, String detail, String instance) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setType(URI.create(TYPE_BASE + code));
    if (instance != null && !instance.isBlank()) {
      problem.setInstance(URI.create(instance));
    }
    String requestId = MDC.get(RequestIdFilter.MDC_KEY);
    if (requestId != null) {
      problem.setProperty("requestId", requestId);
    }
    return problem;
  }
}
