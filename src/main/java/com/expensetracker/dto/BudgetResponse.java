package com.expensetracker.dto;

import java.math.BigDecimal;

public record BudgetResponse(
        Long id,
        int month,
        int year,
        BigDecimal amount,
        BigDecimal used,
        BigDecimal remaining,
        double percentUsed) {
}
