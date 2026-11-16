package com.smarthospital.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ProblemDetailsFactory problemDetailsFactory;

    public GlobalExceptionHandler(ProblemDetailsFactory problemDetailsFactory) {
        this.problemDetailsFactory = problemDetailsFactory;
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(
            ApiException exception,
            HttpServletRequest request) {
        return response(
                exception.status(),
                exception.code(),
                exception.getMessage(),
                request.getRequestURI(),
                exception.fieldErrors());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ProblemDetail> handleRateLimitExceeded(
            RateLimitExceededException exception,
            HttpServletRequest request) {
        ProblemDetail problem = problemDetailsFactory.create(
                exception.status(),
                exception.code(),
                exception.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(exception.status())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                fieldErrors.putIfAbsent(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()));
        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_JSON",
                "Request body is malformed or contains unsupported fields",
                request.getRequestURI());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ProblemDetail> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed",
                request.getRequestURI(),
                Map.of(exception.getParameterName(), "Parameter is required"));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Authentication is required",
                request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Access is denied",
                request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed",
                request.getRequestURI(),
                Map.of(exception.getName(), "Value has an invalid format"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                "Request media type is not supported",
                request.getRequestURI());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<ProblemDetail> handleNotAcceptable(
            HttpMediaTypeNotAcceptableException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.NOT_ACCEPTABLE,
                "NOT_ACCEPTABLE",
                "Response media type is not acceptable",
                request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleDataIntegrity(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT,
                "ENTITY_CONFLICT",
                "The request conflicts with existing data",
                request.getRequestURI());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(
            NoResourceFoundException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                "Resource was not found",
                request.getRequestURI());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "HTTP method is not supported for this resource",
                request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        LOGGER.error(
                "Unhandled request failure for {} ({})\n{}",
                request.getRequestURI(),
                exception.getClass().getName(),
                sanitizedStackTrace(exception));
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                request.getRequestURI());
    }

    private static String sanitizedStackTrace(Exception exception) {
        StackTraceElement[] stackTrace = exception.getStackTrace();
        int frameCount = Math.min(stackTrace.length, 64);
        StringBuilder sanitized = new StringBuilder();
        for (int index = 0; index < frameCount; index++) {
            sanitized.append(System.lineSeparator())
                    .append("at ")
                    .append(stackTrace[index]);
        }
        return sanitized.toString();
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status,
            String code,
            String message,
            String path) {
        return response(status, code, message, path, Map.of());
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status,
            String code,
            String message,
            String path,
            Map<String, String> fieldErrors) {
        ProblemDetail problem = problemDetailsFactory.create(
                status,
                code,
                message,
                path,
                fieldErrors);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
