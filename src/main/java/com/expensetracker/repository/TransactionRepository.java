package com.expensetracker.repository;

import com.expensetracker.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    List<Transaction> findByUserId(Long userId);

    List<Transaction> findByUserIdAndDateBetween(Long userId, LocalDate from, LocalDate to);

    List<Transaction> findTop5ByUserIdOrderByDateDescIdDesc(Long userId);
}
