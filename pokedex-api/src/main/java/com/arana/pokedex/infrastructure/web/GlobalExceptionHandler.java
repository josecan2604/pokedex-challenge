package com.arana.pokedex.infrastructure.web;

import com.arana.pokedex.domain.exception.ConflictException;
import com.arana.pokedex.domain.exception.ExternalServiceException;
import com.arana.pokedex.domain.exception.ResourceNotFoundException;
import com.arana.pokedex.domain.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(ValidationException.class)
    ProblemDetail handleValidation(ValidationException e, HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "The request contains invalid values", request);
        problem.setProperty("errors", e.getErrors());
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "The request contains invalid values", request);
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Parameter '%s' has an invalid value".formatted(e.getName()), request);
        problem.setProperty("errors", Map.of(e.getName(), "invalid value '" + e.getValue() + "'"));
        return problem;
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException e, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    @ExceptionHandler(ExternalServiceException.class)
    ProblemDetail handleExternalService(ExternalServiceException e, HttpServletRequest request) {
        return problem(HttpStatus.BAD_GATEWAY, e.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException e,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> errors.putIfAbsent(f.getField(), f.getDefaultMessage()));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "The request contains invalid values", request);
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(@NonNull HandlerMethodValidationException e,
                                                                            @NonNull HttpHeaders headers,
                                                                            @NonNull HttpStatusCode status,
                                                                            @NonNull WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getAllErrors().forEach(err -> errors.putIfAbsent(
                err instanceof org.springframework.validation.FieldError f ? f.getField() : "request",
                err.getDefaultMessage()));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "The request contains invalid values", request);
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(@NonNull HttpMessageNotReadableException e,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        return ResponseEntity.badRequest()
                .body(problem(HttpStatus.BAD_REQUEST, "The request body is missing or is not valid JSON", request));
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, @NonNull HttpHeaders headers,
                                                          @NonNull HttpStatusCode statusCode, @NonNull WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            enrich(problem, path(request));
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private static ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        enrich(problem, request.getRequestURI());
        return problem;
    }

    private static ProblemDetail problem(HttpStatus status, String detail, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        enrich(problem, path(request));
        return problem;
    }

    private static void enrich(ProblemDetail problem, String path) {
        if (path != null && problem.getInstance() == null) {
            problem.setInstance(URI.create(path));
        }
        problem.setProperty("timestamp", Instant.now());
    }

    private static String path(WebRequest request) {
        return request instanceof ServletWebRequest servlet ? servlet.getRequest().getRequestURI() : null;
    }
}
