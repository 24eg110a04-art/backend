package com.expensetracker.service;

import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/** Small helpers shared by the dashboard, report and budget services. */
final class Totals {

    private Totals() {
    }

    static BigDecimal sum(Collection<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
