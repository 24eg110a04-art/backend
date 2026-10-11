package com.expensetracker.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(boolean success, String message, Instant timestamp, Map<String, String> errors) {

    public static ErrorResponse of(String message) {
        return new ErrorResponse(false, message, Instant.now(), null);
    }

    public static ErrorResponse of(String message, Map<String, String> errors) {
        return new ErrorResponse(false, message, Instant.now(), errors);
    }
}
