package com.expensetracker.controller;

import com.expensetracker.config.AuthUser;
import com.expensetracker.dto.MessageResponse;
import com.expensetracker.dto.PageResponse;
import com.expensetracker.dto.TransactionRequest;
import com.expensetracker.dto.TransactionResponse;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public PageResponse<TransactionResponse> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "type", required = false) TransactionType type,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return transactionService.list(user.id(), search, type, category, from, to, page, size);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable("id") Long id) {
        return transactionService.get(user.id(), id);
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@AuthenticationPrincipal AuthUser user,
                                                      @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(user.id(), request));
    }

    @PutMapping("/{id}")
    public TransactionResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable("id") Long id,
                                      @Valid @RequestBody TransactionRequest request) {
        return transactionService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    public MessageResponse delete(@AuthenticationPrincipal AuthUser user, @PathVariable("id") Long id) {
        transactionService.delete(user.id(), id);
        return MessageResponse.ok("Transaction deleted");
    }
}
