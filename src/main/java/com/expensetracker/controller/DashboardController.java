package com.expensetracker.controller;

import com.expensetracker.config.AuthUser;
import com.expensetracker.dto.CategoryTotal;
import com.expensetracker.dto.DashboardSummary;
import com.expensetracker.dto.PeriodTotal;
import com.expensetracker.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummary summary(@AuthenticationPrincipal AuthUser user) {
        return dashboardService.summary(user.id());
    }

    @GetMapping("/monthly")
    public List<PeriodTotal> monthly(@AuthenticationPrincipal AuthUser user,
                                     @RequestParam(name = "months", defaultValue = "6") int months) {
        return dashboardService.monthly(user.id(), months);
    }

    @GetMapping("/categories")
    public List<CategoryTotal> categories(@AuthenticationPrincipal AuthUser user,
                                          @RequestParam(name = "month", required = false) Integer month,
                                          @RequestParam(name = "year", required = false) Integer year) {
        return dashboardService.categories(user.id(), month, year);
    }
}
