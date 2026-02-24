package com.covenantiq.service;

import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.dto.response.RiskSummaryResponse;
import com.covenantiq.enums.AlertType;
import com.covenantiq.enums.CovenantResultStatus;
import com.covenantiq.enums.RiskLevel;
import com.covenantiq.enums.SeverityLevel;
import com.covenantiq.repository.AlertRepository;
import com.covenantiq.repository.CovenantRepository;
import com.covenantiq.repository.CovenantResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskSummaryService {

    private final FinancialStatementService financialStatementService;
    private final CovenantRepository covenantRepository;
    private final CovenantResultRepository covenantResultRepository;
    private final AlertRepository alertRepository;

    public RiskSummaryService(
            FinancialStatementService financialStatementService,
            CovenantRepository covenantRepository,
            CovenantResultRepository covenantResultRepository,
            AlertRepository alertRepository
    ) {
        this.financialStatementService = financialStatementService;
        this.covenantRepository = covenantRepository;
        this.covenantResultRepository = covenantResultRepository;
        this.alertRepository = alertRepository;
    }

    @Transactional(readOnly = true)
    public RiskSummaryResponse getRiskSummary(Long loanId) {
        long totalCovenants = covenantRepository.findByLoanIdOrderByIdAsc(loanId).size();
        FinancialStatement latest = financialStatementService.getLatestStatement(loanId);

        if (latest == null) {
            return new RiskSummaryResponse(totalCovenants, 0, 0, RiskLevel.LOW);
        }

        long breachedCount = covenantResultRepository.countByFinancialStatementIdAndStatusAndSupersededFalse(
                latest.getId(), CovenantResultStatus.BREACH
        );
        long activeWarnings = alertRepository.countByFinancialStatementIdAndAlertTypeAndSupersededFalse(
                latest.getId(), AlertType.EARLY_WARNING
        );

        boolean anyHighBreach = alertRepository.existsByFinancialStatementIdAndAlertTypeAndSeverityLevelAndSupersededFalse(
                latest.getId(), AlertType.BREACH, SeverityLevel.HIGH
        );

        RiskLevel level;
        if (anyHighBreach) {
            level = RiskLevel.HIGH;
        } else if (breachedCount > 0 || activeWarnings > 0) {
            level = RiskLevel.MEDIUM;
        } else {
            level = RiskLevel.LOW;
        }

        return new RiskSummaryResponse(totalCovenants, breachedCount, activeWarnings, level);
    }
}
