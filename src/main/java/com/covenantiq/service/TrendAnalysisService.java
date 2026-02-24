package com.covenantiq.service;

import com.covenantiq.domain.Covenant;
import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.domain.Loan;
import com.covenantiq.enums.AlertType;
import com.covenantiq.enums.CovenantType;
import com.covenantiq.enums.PeriodType;
import com.covenantiq.enums.SeverityLevel;
import com.covenantiq.repository.CovenantRepository;
import com.covenantiq.repository.FinancialStatementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TrendAnalysisService {

    private static final BigDecimal LOWER_BAND = new BigDecimal("0.95");
    private static final BigDecimal UPPER_BAND = new BigDecimal("1.05");

    private final FinancialStatementRepository financialStatementRepository;
    private final CovenantRepository covenantRepository;
    private final FinancialRatioService financialRatioService;
    private final AlertService alertService;

    public TrendAnalysisService(
            FinancialStatementRepository financialStatementRepository,
            CovenantRepository covenantRepository,
            FinancialRatioService financialRatioService,
            AlertService alertService
    ) {
        this.financialStatementRepository = financialStatementRepository;
        this.covenantRepository = covenantRepository;
        this.financialRatioService = financialRatioService;
        this.alertService = alertService;
    }

    @Transactional
    public void evaluateAndPersist(Loan loan, FinancialStatement statement) {
        runConsecutiveDeclineRule(loan, statement);
        runNearThresholdRule(loan, statement);
    }

    private void runConsecutiveDeclineRule(Loan loan, FinancialStatement statement) {
        List<FinancialStatement> statements = financialStatementRepository
                .findByLoanIdAndPeriodTypeAndSupersededFalseOrderByFiscalYearAscFiscalQuarterAscIdAsc(
                        loan.getId(),
                        statement.getPeriodType()
                );

        if (statements.size() < 3) {
            return;
        }

        int last = statements.size() - 1;
        BigDecimal ratio1 = financialRatioService.calculateCurrentRatio(statements.get(last - 2));
        BigDecimal ratio2 = financialRatioService.calculateCurrentRatio(statements.get(last - 1));
        BigDecimal ratio3 = financialRatioService.calculateCurrentRatio(statements.get(last));

        if (ratio1.compareTo(ratio2) > 0 && ratio2.compareTo(ratio3) > 0) {
            alertService.createAlert(
                    loan,
                    statement,
                    AlertType.EARLY_WARNING,
                    "Current ratio declined for three consecutive " + label(statement.getPeriodType()) + " statements",
                    SeverityLevel.MEDIUM,
                    "CURRENT_RATIO_3_DECLINE"
            );
        }
    }

    private void runNearThresholdRule(Loan loan, FinancialStatement statement) {
        List<Covenant> covenants = covenantRepository.findByLoanIdOrderByIdAsc(loan.getId());
        for (Covenant covenant : covenants) {
            BigDecimal actual = covenant.getType() == CovenantType.CURRENT_RATIO
                    ? financialRatioService.calculateCurrentRatio(statement)
                    : financialRatioService.calculateDebtToEquity(statement);

            BigDecimal lower = covenant.getThresholdValue().multiply(LOWER_BAND);
            BigDecimal upper = covenant.getThresholdValue().multiply(UPPER_BAND);
            boolean withinBand = actual.compareTo(lower) >= 0 && actual.compareTo(upper) <= 0;

            if (withinBand) {
                alertService.createAlert(
                        loan,
                        statement,
                        AlertType.EARLY_WARNING,
                        "Ratio near threshold for " + covenant.getType() + ". Actual=" + actual
                                + ", Threshold=" + covenant.getThresholdValue(),
                        SeverityLevel.MEDIUM,
                        "NEAR_THRESHOLD_" + covenant.getType()
                );
            }
        }
    }

    private String label(PeriodType periodType) {
        return periodType == PeriodType.QUARTERLY ? "quarterly" : "annual";
    }
}
