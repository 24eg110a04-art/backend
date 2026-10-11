package com.expensetracker.controller;

import com.expensetracker.config.AuthUser;
import com.expensetracker.dto.CategoryTotal;
import com.expensetracker.dto.PeriodTotal;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/daily")
    public List<PeriodTotal> daily(@AuthenticationPrincipal AuthUser user,
                                   @RequestParam(name = "days", defaultValue = "14") int days) {
        return reportService.daily(user.id(), days);
    }

    @GetMapping("/weekly")
    public List<PeriodTotal> weekly(@AuthenticationPrincipal AuthUser user,
                                    @RequestParam(name = "weeks", defaultValue = "8") int weeks) {
        return reportService.weekly(user.id(), weeks);
    }

    @GetMapping("/monthly")
    public List<PeriodTotal> monthly(@AuthenticationPrincipal AuthUser user,
                                     @RequestParam(name = "months", defaultValue = "12") int months) {
        return reportService.monthly(user.id(), months);
    }

    @GetMapping("/category")
    public List<CategoryTotal> category(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(name = "type", defaultValue = "EXPENSE") TransactionType type,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.categoryTotals(user.id(), type, from, to);
    }
}
