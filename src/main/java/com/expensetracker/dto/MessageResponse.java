package com.expensetracker.dto;

import java.time.Instant;

public record MessageResponse(boolean success, String message, Instant timestamp) {
    public static MessageResponse ok(String message) {
        return new MessageResponse(true, message, Instant.now());
    }
}
