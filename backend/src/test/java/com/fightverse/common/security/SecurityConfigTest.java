package com.fightverse.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTest {

  private static final String SECRET = "0123456789abcdef0123456789abcdef";

  private final SecurityConfig config = new SecurityConfig();
  private final SecurityProperties properties =
      new SecurityProperties("fightverse", SECRET, Duration.ofMinutes(15), Duration.ofDays(7));

  @Test
  void passwordEncoderIsBcrypt() {
    PasswordEncoder encoder = config.passwordEncoder();

    String hash = encoder.encode("correct-horse-battery");

    assertThat(hash).isNotEqualTo("correct-horse-battery");
    assertThat(hash).startsWith("$2");
    assertThat(encoder.matches("correct-horse-battery", hash)).isTrue();
    assertThat(encoder.matches("wrong-password", hash)).isFalse();
  }

  @Test
  void jwtEncoderAndDecoderRoundTrip() {
    JwtEncoder encoder = config.jwtEncoder(properties);
    JwtDecoder decoder = config.jwtDecoder(properties);
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("fightverse")
            .subject("user-1")
            .claim("roles", List.of("ADMIN"))
            .build();

    String token = encode(encoder, claims);
    Jwt decoded = decoder.decode(token);

    assertThat(decoded.getSubject()).isEqualTo("user-1");
    assertThat(decoded.getClaimAsStringList("roles")).containsExactly("ADMIN");
  }

  @Test
  void decoderRejectsTokensSignedWithAnotherKey() {
    SecurityProperties other =
        new SecurityProperties(
            "fightverse",
            "ffffffffffffffffffffffffffffffff",
            Duration.ofMinutes(15),
            Duration.ofDays(7));
    String token =
        encode(config.jwtEncoder(other), JwtClaimsSet.builder().subject("user-1").build());

    assertThatThrownBy(() -> config.jwtDecoder(properties).decode(token))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void jwtAuthenticationConverterMapsRolesToPrefixedAuthorities() {
    JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .subject("user-1")
            .claim("roles", List.of("ADMIN", "STUDENT"))
            .build();

    AbstractAuthenticationToken authentication = converter.convert(jwt);

    // Spring Security 7 adds its own factor authority, so compare only the role-derived ones.
    assertThat(roleAuthorities(authentication))
        .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_STUDENT");
  }

  @Test
  void jwtAuthenticationConverterYieldsNoAuthoritiesWithoutRolesClaim() {
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject("user-1").build();

    AbstractAuthenticationToken authentication = config.jwtAuthenticationConverter().convert(jwt);

    assertThat(roleAuthorities(authentication)).isEmpty();
  }

  @Test
  void corsConfigurationAllowsConfiguredOrigins() {
    CorsConfigurationSource source =
        config.corsConfigurationSource(new WebProperties(List.of("https://fightverse.app")));

    CorsConfiguration cors =
        source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/techniques"));

    assertThat(cors).isNotNull();
    assertThat(cors.getAllowedOrigins()).containsExactly("https://fightverse.app");
    assertThat(cors.getAllowedMethods())
        .containsExactly("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS");
    assertThat(cors.getExposedHeaders()).containsExactly("X-Request-Id");
  }

  private static String encode(JwtEncoder encoder, JwtClaimsSet claims) {
    // The production encoder is configured with an ImmutableSecret, so the algorithm must be
    // supplied explicitly for a signing key to be selected.
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  private static List<String> roleAuthorities(AbstractAuthenticationToken authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(authority -> authority.startsWith("ROLE_"))
        .toList();
  }
}
