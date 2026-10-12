package com.colortinting.api;

import com.colortinting.domain.Quantities;
import org.springframework.http.HttpStatus;

import java.util.List;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;
    private final List<Quantities.FieldViolation> fieldErrors;

    public ApiException(HttpStatus status, String errorCode, String message) {
        this(status, errorCode, message, List.of());
    }

    public ApiException(
            HttpStatus status,
            String errorCode,
            String message,
            List<Quantities.FieldViolation> fieldErrors
    ) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors;
    }

    public HttpStatus status() {
        return status;
    }

    public String errorCode() {
        return errorCode;
    }

    public List<Quantities.FieldViolation> fieldErrors() {
        return fieldErrors;
    }
}
