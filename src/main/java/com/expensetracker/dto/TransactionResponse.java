package com.expensetracker.dto;

import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        String category,
        LocalDate date,
        String paymentMethod,
        String description,
        LocalDateTime createdAt) {

    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.getId(), t.getType(), t.getAmount(), t.getCategory(),
                t.getDate(), t.getPaymentMethod(), t.getDescription(), t.getCreatedAt());
    }
}
