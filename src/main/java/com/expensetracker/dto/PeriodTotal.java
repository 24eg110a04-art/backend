package com.expensetracker.dto;

import java.math.BigDecimal;

/** Income and expense totals for one period (a day, a week starting on "label", or a month). */
public record PeriodTotal(String label, BigDecimal income, BigDecimal expense) {
}
