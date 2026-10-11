package com.expensetracker.dto;

import com.expensetracker.entity.TransactionType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotNull(message = "Transaction type is required")
        TransactionType type,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @DecimalMax(value = "9999999999.99", message = "Amount is too large")
        BigDecimal amount,

        @NotBlank(message = "Category is required")
        @Size(max = 50, message = "Category must be at most 50 characters")
        String category,

        @NotNull(message = "Date is required")
        LocalDate date,

        @Size(max = 30, message = "Payment method must be at most 30 characters")
        String paymentMethod,

        @Size(max = 255, message = "Description must be at most 255 characters")
        String description) {
}
