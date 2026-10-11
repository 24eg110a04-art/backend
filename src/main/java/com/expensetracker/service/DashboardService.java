package com.expensetracker.service;

import com.expensetracker.dto.CategoryTotal;
import com.expensetracker.dto.DashboardSummary;
import com.expensetracker.dto.PeriodTotal;
import com.expensetracker.dto.TransactionResponse;
import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.exception.ValidationException;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final ReportService reportService;

    public DashboardService(TransactionRepository transactionRepository, BudgetRepository budgetRepository,
                            ReportService reportService) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.reportService = reportService;
    }

    /**
     * Totals and balance cover all transactions. Budget, highest-spending category and average
     * daily expense are for the current month.
     */
    @Transactional(readOnly = true)
    public DashboardSummary summary(Long userId) {
        List<Transaction> all = transactionRepository.findByUserId(userId);
        BigDecimal totalIncome = Totals.sum(all, TransactionType.INCOME);
        BigDecimal totalExpenses = Totals.sum(all, TransactionType.EXPENSE);

        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);
        List<Transaction> monthTransactions = all.stream()
                .filter(t -> YearMonth.from(t.getDate()).equals(thisMonth))
                .toList();
        BigDecimal monthlyExpenses = Totals.sum(monthTransactions, TransactionType.EXPENSE);

        BigDecimal monthlyBudget = budgetRepository
                .findByUserIdAndMonthAndYear(userId, thisMonth.getMonthValue(), thisMonth.getYear())
                .map(b -> b.getAmount().setScale(2, RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO.setScale(2));

        List<CategoryTotal> monthCategories = reportService.categoryTotals(
                userId, TransactionType.EXPENSE, thisMonth.atDay(1), thisMonth.atEndOfMonth());
        String topCategory = monthCategories.isEmpty() ? null : monthCategories.get(0).category();
        BigDecimal topAmount = monthCategories.isEmpty() ? BigDecimal.ZERO.setScale(2) : monthCategories.get(0).total();

        BigDecimal averageDaily = monthlyExpenses
                .divide(BigDecimal.valueOf(today.getDayOfMonth()), 2, RoundingMode.HALF_UP);

        List<TransactionResponse> recent = transactionRepository
                .findTop5ByUserIdOrderByDateDescIdDesc(userId).stream()
                .map(TransactionResponse::from)
                .toList();

        return new DashboardSummary(totalIncome, totalExpenses, totalIncome.subtract(totalExpenses),
                thisMonth.getMonthValue(), thisMonth.getYear(), monthlyBudget, monthlyExpenses,
                monthlyBudget.subtract(monthlyExpenses), topCategory, topAmount, averageDaily,
                all.size(), recent);
    }

    /** Income and expenses for each of the last {@code months} months. */
    public List<PeriodTotal> monthly(Long userId, int months) {
        return reportService.monthly(userId, months);
    }

    /** Expense totals per category for one month (defaults to the current month). */
    public List<CategoryTotal> categories(Long userId, Integer month, Integer year) {
        YearMonth ym = YearMonth.now();
        if (month != null || year != null) {
            int m = month != null ? month : ym.getMonthValue();
            int y = year != null ? year : ym.getYear();
            if (m < 1 || m > 12) {
                throw new ValidationException("Month must be between 1 and 12");
            }
            if (y < 2000 || y > 2100) {
                throw new ValidationException("Year must be between 2000 and 2100");
            }
            ym = YearMonth.of(y, m);
        }
        return reportService.categoryTotals(userId, TransactionType.EXPENSE, ym.atDay(1), ym.atEndOfMonth());
    }
}
