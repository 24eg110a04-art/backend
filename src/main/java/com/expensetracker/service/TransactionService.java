package com.expensetracker.service;

import com.expensetracker.dto.PageResponse;
import com.expensetracker.dto.TransactionRequest;
import com.expensetracker.dto.TransactionResponse;
import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.exception.ValidationException;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> list(Long userId, String search, TransactionType type,
                                                  String category, LocalDate from, LocalDate to,
                                                  int page, int size) {
        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category.trim()));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("date"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDate>get("date"), to));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("description")), like),
                        cb.like(cb.lower(root.<String>get("category")), like),
                        cb.like(cb.lower(root.<String>get("paymentMethod")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(Totals.clamp(page, 0, 100000), Totals.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, "date").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<Transaction> result = transactionRepository.findAll(spec, pageable);
        return PageResponse.of(result, TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(Long userId, Long id) {
        return TransactionResponse.from(findOwned(userId, id));
    }

    @Transactional
    public TransactionResponse create(Long userId, TransactionRequest request) {
        validateDate(request.date());
        Transaction transaction = new Transaction();
        transaction.setUser(userRepository.getReferenceById(userId));
        apply(transaction, request);
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse update(Long userId, Long id, TransactionRequest request) {
        validateDate(request.date());
        Transaction transaction = findOwned(userId, id);
        apply(transaction, request);
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        transactionRepository.delete(findOwned(userId, id));
    }

    private Transaction findOwned(Long userId, Long id) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }

    private void apply(Transaction transaction, TransactionRequest request) {
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setCategory(request.category().trim());
        transaction.setDate(request.date());
        transaction.setPaymentMethod(blankToNull(request.paymentMethod()));
        transaction.setDescription(blankToNull(request.description()));
    }

    private void validateDate(LocalDate date) {
        if (date.isBefore(LocalDate.of(2000, 1, 1))) {
            throw new ValidationException("Transaction date must be 2000-01-01 or later");
        }
        if (date.isAfter(LocalDate.now().plusDays(1))) {
            throw new ValidationException("Transaction date cannot be in the future");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
