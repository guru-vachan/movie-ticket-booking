package com.guru.movie_ticket_booking;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
class ApiErrorHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> api(ApiException e) {
        return ResponseEntity.status(e.status).body(body(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream().findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage()).orElse("Invalid request");
        return ResponseEntity.badRequest().body(body(message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> duplicate() {
        return ResponseEntity.status(409).body(body("Resource already exists"));
    }

    private Map<String, Object> body(String message) {
        return Map.of("timestamp", Instant.now(), "message", message);
    }
}
