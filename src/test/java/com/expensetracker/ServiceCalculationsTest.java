package com.expensetracker;

import com.expensetracker.dto.*;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.entity.User;
import com.expensetracker.exception.ValidationException;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.service.BudgetService;
import com.expensetracker.service.DashboardService;
import com.expensetracker.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ServiceCalculationsTest {

    @Autowired UserRepository userRepository;
    @Autowired TransactionService transactionService;
    @Autowired DashboardService dashboardService;
    @Autowired BudgetService budgetService;

    private Long userId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setName("Calc User");
        user.setEmail("calc@test.com");
        user.setPassword("not-used");
        userId = userRepository.save(user).getId();
    }

    private void add(TransactionType type, String amount, String category) {
        transactionService.create(userId, new TransactionRequest(type, new BigDecimal(amount), category,
                LocalDate.now(), "Cash", "test"));
    }

    @Test
    void dashboardSummary_calculatesTotalsBalanceAndBudget() {
        add(TransactionType.INCOME, "10000", "Salary");
        add(TransactionType.INCOME, "2500.50", "Freelance");
        add(TransactionType.EXPENSE, "1200", "Food");
        add(TransactionType.EXPENSE, "800", "Rent");
        add(TransactionType.EXPENSE, "300", "Food");
        LocalDate today = LocalDate.now();
        budgetService.create(userId, new BudgetRequest(today.getMonthValue(), today.getYear(), new BigDecimal("5000")));

        DashboardSummary s = dashboardService.summary(userId);

        assertThat(s.totalIncome()).isEqualByComparingTo("12500.50");
        assertThat(s.totalExpenses()).isEqualByComparingTo("2300");
        assertThat(s.balance()).isEqualByComparingTo("10200.50");
        assertThat(s.monthlyBudget()).isEqualByComparingTo("5000");
        assertThat(s.monthlyExpenses()).isEqualByComparingTo("2300");
        assertThat(s.remainingBudget()).isEqualByComparingTo("2700");
        assertThat(s.highestSpendingCategory()).isEqualTo("Food");
        assertThat(s.highestSpendingAmount()).isEqualByComparingTo("1500");
        assertThat(s.transactionCount()).isEqualTo(5);
        assertThat(s.averageDailyExpense())
                .isEqualByComparingTo(new BigDecimal("2300").divide(BigDecimal.valueOf(today.getDayOfMonth()), 2,
                        java.math.RoundingMode.HALF_UP));
    }

    @Test
    void dashboardCategoriesAndMonthly_groupCorrectly() {
        add(TransactionType.EXPENSE, "100", "Food");
        add(TransactionType.EXPENSE, "300", "Travel");
        add(TransactionType.INCOME, "1000", "Salary");

        List<CategoryTotal> categories = dashboardService.categories(userId, null, null);
        assertThat(categories).extracting(CategoryTotal::category).containsExactly("Travel", "Food");
        assertThat(categories.get(0).percentage()).isEqualTo(75.0);

        List<PeriodTotal> monthly = dashboardService.monthly(userId, 3);
        assertThat(monthly).hasSize(3);
        PeriodTotal current = monthly.get(2);
        assertThat(current.income()).isEqualByComparingTo("1000");
        assertThat(current.expense()).isEqualByComparingTo("400");
    }

    @Test
    void budget_calculatesUsedRemainingAndPercent() {
        LocalDate today = LocalDate.now();
        add(TransactionType.EXPENSE, "250", "Food");
        add(TransactionType.EXPENSE, "150", "Transport");
        add(TransactionType.INCOME, "9999", "Salary"); // income must not count as spending

        BudgetResponse b = budgetService.create(userId,
                new BudgetRequest(today.getMonthValue(), today.getYear(), new BigDecimal("1000")));

        assertThat(b.used()).isEqualByComparingTo("400");
        assertThat(b.remaining()).isEqualByComparingTo("600");
        assertThat(b.percentUsed()).isEqualTo(40.0);
        assertThat(budgetService.list(userId)).hasSize(1);
    }

    @Test
    void budget_overspent_hasNegativeRemaining_andDuplicateIsRejected() {
        LocalDate today = LocalDate.now();
        add(TransactionType.EXPENSE, "1500", "Rent");
        BudgetRequest request = new BudgetRequest(today.getMonthValue(), today.getYear(), new BigDecimal("1000"));

        BudgetResponse b = budgetService.create(userId, request);
        assertThat(b.remaining()).isEqualByComparingTo("-500");
        assertThat(b.percentUsed()).isEqualTo(150.0);

        assertThatThrownBy(() -> budgetService.create(userId, request)).isInstanceOf(ValidationException.class);
    }
}
