package com.expensetracker.service;

import com.expensetracker.dto.CategoryTotal;
import com.expensetracker.dto.PeriodTotal;
import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /** One entry per day for the last {@code days} days (label = ISO date). */
    @Transactional(readOnly = true)
    public List<PeriodTotal> daily(Long userId, int days) {
        int n = Totals.clamp(days, 1, 90);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(n - 1L);
        List<Transaction> all = transactionRepository.findByUserIdAndDateBetween(userId, start, end);

        List<PeriodTotal> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            LocalDate day = start.plusDays(i);
            List<Transaction> inDay = all.stream().filter(t -> t.getDate().equals(day)).toList();
            result.add(new PeriodTotal(day.toString(),
                    Totals.sum(inDay, TransactionType.INCOME), Totals.sum(inDay, TransactionType.EXPENSE)));
        }
        return result;
    }

    /** One entry per week (Monday to Sunday) for the last {@code weeks} weeks (label = Monday's ISO date). */
    @Transactional(readOnly = true)
    public List<PeriodTotal> weekly(Long userId, int weeks) {
        int n = Totals.clamp(weeks, 1, 52);
        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate start = thisMonday.minusWeeks(n - 1L);
        List<Transaction> all = transactionRepository.findByUserIdAndDateBetween(userId, start, thisMonday.plusDays(6));

        List<PeriodTotal> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            LocalDate weekStart = start.plusWeeks(i);
            LocalDate weekEnd = weekStart.plusDays(6);
            List<Transaction> inWeek = all.stream()
                    .filter(t -> !t.getDate().isBefore(weekStart) && !t.getDate().isAfter(weekEnd))
                    .toList();
            result.add(new PeriodTotal(weekStart.toString(),
                    Totals.sum(inWeek, TransactionType.INCOME), Totals.sum(inWeek, TransactionType.EXPENSE)));
        }
        return result;
    }

    /** One entry per calendar month for the last {@code months} months (label = "Oct 2026"). */
    @Transactional(readOnly = true)
    public List<PeriodTotal> monthly(Long userId, int months) {
        int n = Totals.clamp(months, 1, 36);
        YearMonth end = YearMonth.now();
        YearMonth start = end.minusMonths(n - 1L);
        List<Transaction> all = transactionRepository.findByUserIdAndDateBetween(userId, start.atDay(1), end.atEndOfMonth());

        List<PeriodTotal> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            YearMonth ym = start.plusMonths(i);
            List<Transaction> inMonth = all.stream()
                    .filter(t -> YearMonth.from(t.getDate()).equals(ym))
                    .toList();
            String label = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + ym.getYear();
            result.add(new PeriodTotal(label,
                    Totals.sum(inMonth, TransactionType.INCOME), Totals.sum(inMonth, TransactionType.EXPENSE)));
        }
        return result;
    }

    /** Totals per category (largest first). {@code from} and {@code to} are optional. */
    @Transactional(readOnly = true)
    public List<CategoryTotal> categoryTotals(Long userId, TransactionType type, LocalDate from, LocalDate to) {
        List<Transaction> filtered = transactionRepository.findByUserId(userId).stream()
                .filter(t -> t.getType() == type)
                .filter(t -> from == null || !t.getDate().isBefore(from))
                .filter(t -> to == null || !t.getDate().isAfter(to))
                .toList();

        BigDecimal grandTotal = Totals.sum(filtered, type);
        Map<String, BigDecimal> byCategory = filtered.stream().collect(Collectors.groupingBy(
                Transaction::getCategory, Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

        return byCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new CategoryTotal(e.getKey(), e.getValue().setScale(2, RoundingMode.HALF_UP),
                        grandTotal.signum() == 0 ? 0.0
                                : e.getValue().multiply(BigDecimal.valueOf(100))
                                        .divide(grandTotal, 1, RoundingMode.HALF_UP).doubleValue()))
                .toList();
    }
}
