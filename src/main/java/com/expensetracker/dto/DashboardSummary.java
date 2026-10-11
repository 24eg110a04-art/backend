package com.expensetracker.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummary(
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal balance,
        int month,
        int year,
        BigDecimal monthlyBudget,
        BigDecimal monthlyExpenses,
        BigDecimal remainingBudget,
        String highestSpendingCategory,
        BigDecimal highestSpendingAmount,
        BigDecimal averageDailyExpense,
        long transactionCount,
        List<TransactionResponse> recentTransactions) {
}
