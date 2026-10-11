package com.expensetracker.service;

import com.expensetracker.dto.BudgetRequest;
import com.expensetracker.dto.BudgetResponse;
import com.expensetracker.entity.Budget;
import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.exception.ValidationException;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public BudgetService(BudgetRepository budgetRepository, TransactionRepository transactionRepository,
                         UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(Long userId) {
        return budgetRepository.findByUserIdOrderByYearDescMonthDesc(userId).stream()
                .map(b -> toResponse(userId, b))
                .toList();
    }

    @Transactional
    public BudgetResponse create(Long userId, BudgetRequest request) {
        ensureNoDuplicate(userId, request, null);
        Budget budget = new Budget();
        budget.setUser(userRepository.getReferenceById(userId));
        apply(budget, request);
        return toResponse(userId, budgetRepository.save(budget));
    }

    @Transactional
    public BudgetResponse update(Long userId, Long id, BudgetRequest request) {
        Budget budget = findOwned(userId, id);
        ensureNoDuplicate(userId, request, id);
        apply(budget, request);
        return toResponse(userId, budgetRepository.save(budget));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        budgetRepository.delete(findOwned(userId, id));
    }

    private Budget findOwned(Long userId, Long id) {
        return budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
    }

    private void ensureNoDuplicate(Long userId, BudgetRequest request, Long ignoreId) {
        budgetRepository.findByUserIdAndMonthAndYear(userId, request.month(), request.year())
                .filter(existing -> !existing.getId().equals(ignoreId))
                .ifPresent(existing -> {
                    throw new ValidationException("A budget for " + request.month() + "/" + request.year()
                            + " already exists. Edit it instead.");
                });
    }

    private void apply(Budget budget, BudgetRequest request) {
        budget.setMonth(request.month());
        budget.setYear(request.year());
        budget.setAmount(request.amount());
    }

    private BudgetResponse toResponse(Long userId, Budget budget) {
        YearMonth ym = YearMonth.of(budget.getYear(), budget.getMonth());
        List<Transaction> monthTransactions = transactionRepository
                .findByUserIdAndDateBetween(userId, ym.atDay(1), ym.atEndOfMonth());
        BigDecimal used = Totals.sum(monthTransactions, TransactionType.EXPENSE);
        BigDecimal amount = budget.getAmount().setScale(2, RoundingMode.HALF_UP);
        double percent = used.multiply(BigDecimal.valueOf(100))
                .divide(amount, 1, RoundingMode.HALF_UP).doubleValue();
        return new BudgetResponse(budget.getId(), budget.getMonth(), budget.getYear(), amount, used,
                amount.subtract(used), percent);
    }
}
