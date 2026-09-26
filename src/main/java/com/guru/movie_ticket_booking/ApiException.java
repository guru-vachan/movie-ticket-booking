package com.guru.movie_ticket_booking;

import org.springframework.http.HttpStatus;

class ApiException extends RuntimeException {
    final HttpStatus status;

    ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    static ApiException notFound(String what) {
        return new ApiException(HttpStatus.NOT_FOUND, what + " not found");
    }

    static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
