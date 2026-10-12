package com.colortinting.api;

import com.colortinting.domain.Quantities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.status()).body(body(
                ex.status().value(),
                ex.errorCode(),
                ex.getMessage(),
                ex.fieldErrors().stream().map(v -> new FieldErrorResponse(v.field(), v.message())).toList()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalid(MethodArgumentNotValidException ex) {
        List<FieldErrorResponse> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage() == null
                        ? "Invalid value."
                        : error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(body(400, "VALIDATION_ERROR", "The request is invalid.", fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(body(400, "MALFORMED_REQUEST", "The request body could not be read.", List.of()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation correlationId={}", CorrelationIdFilter.current());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(
                409,
                "DATA_CONFLICT",
                "The request conflicts with existing data.",
                List.of()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled API error correlationId={}", CorrelationIdFilter.current(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body(
                500,
                "INTERNAL_ERROR",
                "The request could not be completed.",
                List.of()
        ));
    }

    private static ApiErrorResponse body(int status, String code, String message, List<FieldErrorResponse> fields) {
        return new ApiErrorResponse(Instant.now(), status, code, message, CorrelationIdFilter.current(), fields);
    }

    public record ApiErrorResponse(
            Instant timestamp,
            int status,
            String errorCode,
            String message,
            String correlationId,
            List<FieldErrorResponse> fieldErrors
    ) {
    }

    public record FieldErrorResponse(String field, String message) {
    }
}
