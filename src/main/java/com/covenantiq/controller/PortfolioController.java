package com.covenantiq.controller;

import com.covenantiq.dto.response.PortfolioSummaryResponse;
import com.covenantiq.dto.response.PortfolioTrendPointResponse;
import com.covenantiq.service.PortfolioSummaryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {

    private final PortfolioSummaryService portfolioSummaryService;

    public PortfolioController(PortfolioSummaryService portfolioSummaryService) {
        this.portfolioSummaryService = portfolioSummaryService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public PortfolioSummaryResponse getSummary() {
        return portfolioSummaryService.getSummary();
    }

    @GetMapping("/trend")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public List<PortfolioTrendPointResponse> getTrend() {
        return portfolioSummaryService.getTrend();
    }
}
