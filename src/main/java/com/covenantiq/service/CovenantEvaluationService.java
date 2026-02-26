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

    public CovenantEvaluationService(
            CovenantRepository covenantRepository,
            CovenantResultRepository covenantResultRepository,
            FinancialRatioService financialRatioService,
            AlertService alertService
    ) {
        this.covenantRepository = covenantRepository;
        this.covenantResultRepository = covenantResultRepository;
        this.financialRatioService = financialRatioService;
        this.alertService = alertService;
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
        saved.stream()
                .filter(r -> r.getStatus() == CovenantResultStatus.BREACH)
                .forEach(r -> alertService.createAlert(
                        loan,
                        statement,
                        AlertType.BREACH,
                        "Covenant breach for " + r.getCovenant().getType() + ". Actual=" + r.getActualValue()
                                + ", Threshold=" + r.getCovenant().getThresholdValue(),
                        r.getCovenant().getSeverityLevel(),
                        "BREACH_" + r.getCovenant().getType()
                ));

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
