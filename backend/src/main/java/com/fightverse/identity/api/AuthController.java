package com.fightverse.identity.api;

import com.fightverse.identity.api.dto.AuthSessionResponse;
import com.fightverse.identity.api.dto.LoginRequest;
import com.fightverse.identity.api.dto.LogoutRequest;
import com.fightverse.identity.api.dto.RefreshTokenRequest;
import com.fightverse.identity.api.dto.RegisterRequest;
import com.fightverse.identity.api.dto.TokenResponse;
import com.fightverse.identity.application.AuthService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public AuthSessionResponse register(
      @Valid @RequestBody RegisterRequest request,
      @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {

    return authService.register(
        request.email(), request.password(), request.displayName(), userAgent);
  }

  @PostMapping("/login")
  public AuthSessionResponse login(
      @Valid @RequestBody LoginRequest request,
      @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {

    return authService.login(request.email(), request.password(), userAgent);
  }

  @PostMapping("/refresh")
  public TokenResponse refresh(
      @Valid @RequestBody RefreshTokenRequest request,
      @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {

    return authService.refresh(request.refreshToken(), userAgent);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody LogoutRequest request) {

    authService.logout(UUID.fromString(jwt.getSubject()), request.refreshToken());
  }
}
