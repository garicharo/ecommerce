package com.garicharo.shop.shared;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> api(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "code", ex.getCode(),
                "message", ex.getMessage(),
                "details", ex.getDetails()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> optimisticLock(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "code", "OPTIMISTIC_LOCK",
                "message", "This product was updated by someone else. Reload and try again.",
                "details", List.of()));
    }
}
