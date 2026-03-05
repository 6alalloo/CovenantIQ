package com.covenantiq.service;

import com.covenantiq.domain.Alert;
import com.covenantiq.domain.CovenantResult;
import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.domain.Loan;
import com.covenantiq.dto.request.SubmitFinancialStatementRequest;
import com.covenantiq.enums.ActivityEventType;
import com.covenantiq.exception.UnprocessableEntityException;
import com.covenantiq.repository.AlertRepository;
import com.covenantiq.repository.CovenantResultRepository;
import com.covenantiq.repository.FinancialStatementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class FinancialStatementService {

    private final LoanService loanService;
    private final FinancialStatementRepository financialStatementRepository;
    private final CovenantResultRepository covenantResultRepository;
    private final AlertRepository alertRepository;
    private final CovenantEvaluationService covenantEvaluationService;
    private final TrendAnalysisService trendAnalysisService;
    private final ActivityLogService activityLogService;
    private final OutboxEventPublisher outboxEventPublisher;

    public FinancialStatementService(
            LoanService loanService,
            FinancialStatementRepository financialStatementRepository,
            CovenantResultRepository covenantResultRepository,
            AlertRepository alertRepository,
            CovenantEvaluationService covenantEvaluationService,
            TrendAnalysisService trendAnalysisService,
            ActivityLogService activityLogService,
            OutboxEventPublisher outboxEventPublisher
    ) {
        this.loanService = loanService;
        this.financialStatementRepository = financialStatementRepository;
        this.covenantResultRepository = covenantResultRepository;
        this.alertRepository = alertRepository;
        this.covenantEvaluationService = covenantEvaluationService;
        this.trendAnalysisService = trendAnalysisService;
        this.activityLogService = activityLogService;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Transactional
    public FinancialStatement submitStatement(Long loanId, SubmitFinancialStatementRequest request) {
        Loan loan = loanService.getLoan(loanId);
        loanService.ensureActive(loan);
        validatePeriod(request);

        financialStatementRepository.findByLoanIdAndPeriodTypeAndFiscalYearAndFiscalQuarterAndSupersededFalse(
                loanId, request.periodType(), request.fiscalYear(), request.fiscalQuarter()
        ).ifPresent(this::supersedeStatementGraph);

        FinancialStatement statement = new FinancialStatement();
        statement.setLoan(loan);
        statement.setPeriodType(request.periodType());
        statement.setFiscalYear(request.fiscalYear());
        statement.setFiscalQuarter(request.fiscalQuarter());
        statement.setCurrentAssets(request.currentAssets());
        statement.setCurrentLiabilities(request.currentLiabilities());
        statement.setTotalDebt(request.totalDebt());
        statement.setTotalEquity(request.totalEquity());
        statement.setEbit(request.ebit());
        statement.setInterestExpense(request.interestExpense());
        statement.setNetOperatingIncome(request.netOperatingIncome());
        statement.setTotalDebtService(request.totalDebtService());
        statement.setIntangibleAssets(request.intangibleAssets());
        statement.setEbitda(request.ebitda());
        statement.setFixedCharges(request.fixedCharges());
        statement.setInventory(request.inventory());
        statement.setTotalAssets(request.totalAssets());
        statement.setTotalLiabilities(request.totalLiabilities());
        statement.setSubmissionTimestampUtc(normalizeToUtc(request.submissionTimestamp()));

        FinancialStatement saved = financialStatementRepository.save(statement);
        covenantEvaluationService.evaluateAndPersist(loan, saved);
        trendAnalysisService.evaluateAndPersist(loan, saved);
        activityLogService.logEvent(
                ActivityEventType.STATEMENT_SUBMITTED,
                "FinancialStatement",
                saved.getId(),
                loanId,
                "Statement submitted for " + request.periodType() + " " + request.fiscalYear()
        );
        outboxEventPublisher.publish("FinancialStatement", saved.getId(), "FinancialStatementSubmitted", java.util.Map.of(
                "loanId", loanId,
                "statementId", saved.getId(),
                "periodType", saved.getPeriodType().name(),
                "fiscalYear", saved.getFiscalYear(),
                "fiscalQuarter", saved.getFiscalQuarter() == null ? "" : saved.getFiscalQuarter().toString()
        ));
        return saved;
    }

    @Transactional(readOnly = true)
    public FinancialStatement getLatestStatement(Long loanId) {
        return financialStatementRepository.findTopByLoanIdAndSupersededFalseOrderBySubmissionTimestampUtcDescIdDesc(loanId)
                .orElse(null);
    }

    private void supersedeStatementGraph(FinancialStatement existingStatement) {
        existingStatement.setSuperseded(true);
        financialStatementRepository.save(existingStatement);
        outboxEventPublisher.publish("FinancialStatement", existingStatement.getId(), "FinancialStatementSuperseded", java.util.Map.of(
                "loanId", existingStatement.getLoan().getId(),
                "statementId", existingStatement.getId()
        ));

        List<CovenantResult> existingResults = covenantResultRepository
                .findByFinancialStatementIdAndSupersededFalse(existingStatement.getId());
        existingResults.forEach(result -> result.setSuperseded(true));
        covenantResultRepository.saveAll(existingResults);

        List<Alert> existingAlerts = alertRepository.findByFinancialStatementIdAndSupersededFalse(existingStatement.getId());
        existingAlerts.forEach(alert -> {
            alert.setSuperseded(true);
            outboxEventPublisher.publish("Alert", alert.getId(), "AlertSuperseded", java.util.Map.of(
                    "loanId", alert.getLoan().getId(),
                    "alertId", alert.getId()
            ));
        });
        alertRepository.saveAll(existingAlerts);
    }

    private void validatePeriod(SubmitFinancialStatementRequest request) {
        switch (request.periodType()) {
            case QUARTERLY -> {
                if (request.fiscalQuarter() == null) {
                    throw new UnprocessableEntityException("fiscalQuarter is required for QUARTERLY statements");
                }
            }
            case ANNUAL -> {
                if (request.fiscalQuarter() != null) {
                    throw new UnprocessableEntityException("fiscalQuarter must be null for ANNUAL statements");
                }
            }
            default -> throw new UnprocessableEntityException("Unsupported periodType");
        }
    }

    private OffsetDateTime normalizeToUtc(OffsetDateTime input) {
        if (input == null) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
        return input.withOffsetSameInstant(ZoneOffset.UTC);
    }
}
