package com.covenantiq.service;

import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.dto.response.CovenantRiskDetailResponse;
import com.covenantiq.dto.response.RiskDetailsResponse;
import com.covenantiq.dto.response.RiskSummaryResponse;
import com.covenantiq.enums.AlertStatus;
import com.covenantiq.enums.AlertType;
import com.covenantiq.enums.CovenantResultStatus;
import com.covenantiq.enums.RiskLevel;
import com.covenantiq.enums.SeverityLevel;
import com.covenantiq.repository.AlertRepository;
import com.covenantiq.repository.CovenantRepository;
import com.covenantiq.repository.CovenantResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
        long activeWarnings = alertRepository.countByFinancialStatementIdAndAlertTypeAndStatusNotAndSupersededFalse(
                latest.getId(), AlertType.EARLY_WARNING, AlertStatus.RESOLVED
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

    @Transactional(readOnly = true)
    public RiskDetailsResponse getRiskDetails(Long loanId) {
        FinancialStatement latest = financialStatementService.getLatestStatement(loanId);
        if (latest == null) {
            return new RiskDetailsResponse(loanId, null, null, List.of());
        }

        List<CovenantRiskDetailResponse> details = new ArrayList<>();
        List<com.covenantiq.domain.CovenantResult> results =
                covenantResultRepository.findByFinancialStatementIdAndSupersededFalseOrderByEvaluationTimestampUtcDescIdDesc(
                        latest.getId()
                );
        List<com.covenantiq.domain.Alert> latestAlerts =
                alertRepository.findByFinancialStatementIdAndSupersededFalseOrderByTriggeredTimestampUtcDescIdDesc(
                        latest.getId()
                );

        for (com.covenantiq.domain.CovenantResult result : results) {
            List<String> ruleCodes = latestAlerts.stream()
                    .filter(a -> a.getMessage().contains(result.getCovenant().getType().name())
                            || a.getAlertRuleCode().contains(result.getCovenant().getType().name()))
                    .map(com.covenantiq.domain.Alert::getAlertRuleCode)
                    .distinct()
                    .toList();

            details.add(new CovenantRiskDetailResponse(
                    result.getCovenant().getId(),
                    result.getCovenant().getType(),
                    result.getCovenant().getThresholdValue(),
                    result.getActualValue(),
                    result.getStatus(),
                    result.getCovenant().getSeverityLevel(),
                    ruleCodes
            ));
        }

        return new RiskDetailsResponse(loanId, latest.getId(), latest.getSubmissionTimestampUtc(), details);
    }
}
