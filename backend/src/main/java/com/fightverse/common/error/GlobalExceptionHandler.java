package com.fightverse.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
    return Problems.of(
        HttpStatus.NOT_FOUND, "not-found", "Resource not found", ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(ConflictException.class)
  ProblemDetail handleConflict(ConflictException ex, HttpServletRequest request) {
    return Problems.of(
        HttpStatus.CONFLICT, "conflict", "Request conflict", ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(InvalidRequestException.class)
  ProblemDetail handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request) {
    return Problems.of(
        HttpStatus.BAD_REQUEST,
        "invalid-request",
        "Invalid request",
        ex.getMessage(),
        request.getRequestURI());
  }

  @ExceptionHandler(AuthenticationException.class)
  ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
    // The reason is deliberately not echoed back: it would let an attacker probe the user store.
    return Problems.of(
        HttpStatus.UNAUTHORIZED,
        "unauthorized",
        "Authentication failed",
        "Invalid credentials.",
        request.getRequestURI());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
    ProblemDetail problem =
        Problems.of(
            HttpStatus.BAD_REQUEST,
            "validation-failed",
            "Validation failed",
            "One or more request parameters are invalid.",
            request.getRequestURI());
    List<Map<String, String>> violations =
        ex.getConstraintViolations().stream()
            .map(
                violation ->
                    Map.of(
                        "field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()))
            .toList();
    problem.setProperty("violations", violations);
    return problem;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
    log.warn("Data integrity violation while processing {}", request.getRequestURI(), ex);
    return Problems.of(
        HttpStatus.CONFLICT,
        "conflict",
        "Request conflict",
        "The request conflicts with existing data.",
        request.getRequestURI());
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
    log.error("Unhandled error while processing {} {}", request.getMethod(), request.getRequestURI(), ex);
    return Problems.of(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "internal-error",
        "Internal server error",
        "An unexpected error occurred. Please retry later.",
        request.getRequestURI());
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    ProblemDetail problem =
        Problems.of(
            HttpStatus.BAD_REQUEST,
            "validation-failed",
            "Validation failed",
            "One or more fields are invalid.",
            path(request));
    List<Map<String, String>> violations =
        ex.getBindingResult().getFieldErrors().stream()
            .map(GlobalExceptionHandler::describe)
            .toList();
    problem.setProperty("violations", violations);
    return ResponseEntity.badRequest().headers(headers).body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    ProblemDetail problem =
        Problems.of(
            HttpStatus.BAD_REQUEST,
            "malformed-request",
            "Malformed request",
            "The request body could not be parsed.",
            path(request));
    return ResponseEntity.badRequest().headers(headers).body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleTypeMismatch(
      TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

    if (!(ex instanceof MethodArgumentTypeMismatchException mismatch)) {
      return super.handleTypeMismatch(ex, headers, status, request);
    }

    ProblemDetail problem =
        Problems.of(
            HttpStatus.BAD_REQUEST,
            "invalid-parameter",
            "Invalid request parameter",
            "Parameter '%s' has an invalid value.".formatted(mismatch.getName()),
            path(request));
    return ResponseEntity.badRequest().headers(headers).body(problem);
  }

  private static Map<String, String> describe(FieldError error) {
    return Map.of(
        "field",
        error.getField(),
        "message",
        Optional.ofNullable(error.getDefaultMessage()).orElse("invalid value"));
  }

  private static String path(WebRequest request) {
    String description = request.getDescription(false);
    return description.startsWith("uri=") ? description.substring(4) : description;
  }
}
