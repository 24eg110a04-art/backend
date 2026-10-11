package com.expensetracker.dto;

import java.math.BigDecimal;

public record CategoryTotal(String category, BigDecimal total, double percentage) {
}
