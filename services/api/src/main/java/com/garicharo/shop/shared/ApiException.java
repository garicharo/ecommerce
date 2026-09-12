package com.garicharo.shop.shared;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final List<Map<String, Object>> details;

    public ApiException(String code, String message, HttpStatus status) {
        this(code, message, status, List.of());
    }

    public ApiException(String code, String message, HttpStatus status, List<Map<String, Object>> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<Map<String, Object>> getDetails() {
        return details;
    }
}
