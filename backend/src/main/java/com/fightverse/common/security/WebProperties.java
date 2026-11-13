package com.fightverse.common.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fightverse.web")
public record WebProperties(List<String> corsAllowedOrigins) {

  public WebProperties {
    corsAllowedOrigins = corsAllowedOrigins == null ? List.of() : List.copyOf(corsAllowedOrigins);
  }
}
