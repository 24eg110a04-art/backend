package com.expensetracker.controller;

import com.expensetracker.config.AuthUser;
import com.expensetracker.dto.BudgetRequest;
import com.expensetracker.dto.BudgetResponse;
import com.expensetracker.dto.MessageResponse;
import com.expensetracker.service.BudgetService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public List<BudgetResponse> list(@AuthenticationPrincipal AuthUser user) {
        return budgetService.list(user.id());
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> create(@AuthenticationPrincipal AuthUser user,
                                                 @Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.create(user.id(), request));
    }

    @PutMapping("/{id}")
    public BudgetResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable("id") Long id,
                                 @Valid @RequestBody BudgetRequest request) {
        return budgetService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    public MessageResponse delete(@AuthenticationPrincipal AuthUser user, @PathVariable("id") Long id) {
        budgetService.delete(user.id(), id);
        return MessageResponse.ok("Budget deleted");
    }
}
