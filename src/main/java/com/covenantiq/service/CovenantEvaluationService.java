package com.covenantiq.service;

import com.covenantiq.domain.Covenant;
import com.covenantiq.domain.CovenantResult;
import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.domain.Loan;
import com.covenantiq.enums.AlertType;
import com.covenantiq.enums.ComparisonType;
import com.covenantiq.enums.CovenantResultStatus;
import com.covenantiq.repository.CovenantRepository;
import com.covenantiq.repository.CovenantResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class CovenantEvaluationService {

    private final CovenantRepository covenantRepository;
    private final CovenantResultRepository covenantResultRepository;
    private final FinancialRatioService financialRatioService;
    private final AlertService alertService;
    private final OutboxEventPublisher outboxEventPublisher;
    private final RulesetService rulesetService;
    private final CollateralExceptionService collateralExceptionService;

    public CovenantEvaluationService(
            CovenantRepository covenantRepository,
            CovenantResultRepository covenantResultRepository,
            FinancialRatioService financialRatioService,
            AlertService alertService,
            OutboxEventPublisher outboxEventPublisher,
            RulesetService rulesetService,
            CollateralExceptionService collateralExceptionService
    ) {
        this.covenantRepository = covenantRepository;
        this.covenantResultRepository = covenantResultRepository;
        this.financialRatioService = financialRatioService;
        this.alertService = alertService;
        this.outboxEventPublisher = outboxEventPublisher;
        this.rulesetService = rulesetService;
        this.collateralExceptionService = collateralExceptionService;
    }

    @Transactional
    public List<CovenantResult> evaluateAndPersist(Loan loan, FinancialStatement statement) {
        List<Covenant> covenants = covenantRepository.findByLoanIdOrderByIdAsc(loan.getId());
        List<CovenantResult> results = new ArrayList<>();

        for (Covenant covenant : covenants) {
            BigDecimal actualValue = getActualValue(covenant, statement);
            boolean pass = compare(actualValue, covenant.getThresholdValue(), covenant.getComparisonType());

            CovenantResult result = new CovenantResult();
            result.setCovenant(covenant);
            result.setFinancialStatement(statement);
            result.setActualValue(actualValue);
            result.setStatus(pass ? CovenantResultStatus.PASS : CovenantResultStatus.BREACH);
            result.setEvaluationTimestampUtc(OffsetDateTime.now(ZoneOffset.UTC));
            results.add(result);
        }

        List<CovenantResult> saved = covenantResultRepository.saveAll(results);
        saved.forEach(result -> {
            outboxEventPublisher.publish("CovenantResult", result.getId(), "CovenantEvaluated", java.util.Map.of(
                    "loanId", loan.getId(),
                    "statementId", statement.getId(),
                    "covenantId", result.getCovenant().getId(),
                    "status", result.getStatus().name(),
                    "severity", result.getCovenant().getSeverityLevel().name()
            ));

            if (result.getStatus() == CovenantResultStatus.BREACH) {
                boolean activeException = collateralExceptionService
                        .getActiveApprovedException(loan.getId(), result.getCovenant().getId())
                        .isPresent();
                RulesetService.RuleDecision decision = rulesetService.evaluatePublished("COVENANT_EVAL_DEFAULT", java.util.Map.of(
                        "comparisonPass", false,
                        "activeException", activeException
                ));
                outboxEventPublisher.publish("CovenantResult", result.getId(), "CovenantBreachDetected", java.util.Map.of(
                        "loanId", loan.getId(),
                        "statementId", statement.getId(),
                        "covenantId", result.getCovenant().getId(),
                        "severity", result.getCovenant().getSeverityLevel().name(),
                        "reasonCode", decision.reasonCode()
                ));
                if (!decision.breach() && activeException) {
                    alertService.createAlert(
                            loan,
                            statement,
                            AlertType.EARLY_WARNING,
                            "Covenant exception active: downgraded breach to warning for " + result.getCovenant().getType(),
                            result.getCovenant().getSeverityLevel(),
                            decision.reasonCode()
                    );
                } else {
                    AlertType alertType = "EARLY_WARNING".equalsIgnoreCase(decision.alertType())
                            ? AlertType.EARLY_WARNING
                            : AlertType.BREACH;
                    alertService.createAlert(
                            loan,
                            statement,
                            alertType,
                            "Covenant breach for " + result.getCovenant().getType() + ". Actual=" + result.getActualValue()
                                    + ", Threshold=" + result.getCovenant().getThresholdValue(),
                            result.getCovenant().getSeverityLevel(),
                            decision.reasonCode()
                    );
                }
            }
        });

        return saved;
    }

    private BigDecimal getActualValue(Covenant covenant, FinancialStatement statement) {
        return financialRatioService.calculateByCovenantType(covenant.getType(), statement);
    }

    private boolean compare(BigDecimal actual, BigDecimal threshold, ComparisonType type) {
        return switch (type) {
            case GREATER_THAN_EQUAL -> actual.compareTo(threshold) >= 0;
            case LESS_THAN_EQUAL -> actual.compareTo(threshold) <= 0;
        };
    }
}
