package com.covenantiq.service;

import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.domain.Loan;
import com.covenantiq.dto.response.PortfolioSummaryResponse;
import com.covenantiq.enums.AlertStatus;
import com.covenantiq.enums.CovenantResultStatus;
import com.covenantiq.enums.LoanStatus;
import com.covenantiq.repository.AlertRepository;
import com.covenantiq.repository.CovenantResultRepository;
import com.covenantiq.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@Service
public class PortfolioSummaryService {

    private final LoanRepository loanRepository;
    private final FinancialStatementService financialStatementService;
    private final CovenantResultRepository covenantResultRepository;
    private final AlertRepository alertRepository;

    public PortfolioSummaryService(
            LoanRepository loanRepository,
            FinancialStatementService financialStatementService,
            CovenantResultRepository covenantResultRepository,
            AlertRepository alertRepository
    ) {
        this.loanRepository = loanRepository;
        this.financialStatementService = financialStatementService;
        this.covenantResultRepository = covenantResultRepository;
        this.alertRepository = alertRepository;
    }

    @Transactional(readOnly = true)
    public PortfolioSummaryResponse getSummary() {
        List<Loan> activeLoans = loanRepository.findAll().stream()
                .filter(loan -> loan.getStatus() == LoanStatus.ACTIVE)
                .toList();

        long totalBreaches = 0;
        long highRisk = 0;
        long mediumRisk = 0;
        long lowRisk = 0;

        for (Loan loan : activeLoans) {
            FinancialStatement latest = financialStatementService.getLatestStatement(loan.getId());
            if (latest == null) {
                lowRisk++;
                continue;
            }
            long breachCount = covenantResultRepository.countByFinancialStatementIdAndStatusAndSupersededFalse(
                    latest.getId(), CovenantResultStatus.BREACH
            );
            totalBreaches += breachCount;
            if (breachCount >= 2) {
                highRisk++;
            } else if (breachCount == 1) {
                mediumRisk++;
            } else {
                lowRisk++;
            }
        }

        long totalOpen = alertRepository.countBySupersededFalseAndStatusIn(EnumSet.of(AlertStatus.OPEN));
        long totalUnderReview = alertRepository.countBySupersededFalseAndStatusIn(EnumSet.of(AlertStatus.UNDER_REVIEW));

        return new PortfolioSummaryResponse(
                activeLoans.size(),
                totalBreaches,
                highRisk,
                mediumRisk,
                lowRisk,
                totalOpen,
                totalUnderReview
        );
    }
}
