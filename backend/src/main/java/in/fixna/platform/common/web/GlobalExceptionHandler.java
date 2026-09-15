package in.fixna.platform.common.web;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions into the standard {@link ApiError} envelope.
 * Internal details are never exposed to callers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger ERROR_LOG = LoggerFactory.getLogger("fixna.error");

    @ExceptionHandler(FixnaException.class)
    public ResponseEntity<ApiError> handleFixna(FixnaException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(build(ex.getStatus(), ex.getCode(), ex.getMessage(), request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", ex.getMessage(), request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Server-side only: callers get the generic envelope; correlation via
        // requestId. Never render ex.getMessage() — it may contain driver,
        // SQL, or provider internals.
        ERROR_LOG.error("Unhandled error requestId={} path={}",
                MDC.get(RequestIdFilter.REQUEST_ID_ATTRIBUTE),
                request != null ? safePath(request) : "<none>", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_ERROR",
                        "An unexpected error occurred",
                        request));
    }

    private ApiError build(HttpStatus status, String code, String message, HttpServletRequest request) {
        return new ApiError(
                OffsetDateTime.now(),
                status.value(),
                code,
                message,
                request != null ? safePath(request) : null,
                MDC.get(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }

    /** Request path only — never query strings (they may carry secrets/tokens). */
    private static String safePath(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
