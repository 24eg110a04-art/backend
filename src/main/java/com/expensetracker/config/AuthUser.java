package com.expensetracker.config;

/** The authenticated user, available in controllers through @AuthenticationPrincipal. */
public record AuthUser(Long id, String email) {
}
