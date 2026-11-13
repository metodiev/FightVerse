package com.fightverse.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Refuses to start a production deployment with the development signing key. A predictable HMAC key
 * lets anyone mint access tokens for any account.
 */
@Configuration
@Profile("prod")
class ProductionSecretGuard {

  private static final Logger log = LoggerFactory.getLogger(ProductionSecretGuard.class);

  @Bean
  ApplicationRunner rejectDevelopmentJwtSecret(SecurityProperties properties) {
    return args -> {
      if (SecurityProperties.DEVELOPMENT_JWT_SECRET.equals(properties.jwtSecret())
          || properties.jwtSecret().startsWith("dev-only")) {
        throw new IllegalStateException(
            "FIGHTVERSE_JWT_SECRET is still the development default. Set a random 256-bit secret before running with the prod profile.");
      }
      log.info("Production security configuration verified (issuer={})", properties.issuer());
    };
  }
}
