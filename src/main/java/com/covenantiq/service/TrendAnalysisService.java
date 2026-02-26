package com.covenantiq.service;

import com.covenantiq.domain.Covenant;
import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.domain.Loan;
import com.covenantiq.enums.AlertType;
import com.covenantiq.enums.PeriodType;
import com.covenantiq.enums.SeverityLevel;
import com.covenantiq.repository.CovenantRepository;
import com.covenantiq.repository.FinancialStatementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class TrendAnalysisService {

    private static final BigDecimal LOWER_BAND = new BigDecimal("0.95");
    private static final BigDecimal UPPER_BAND = new BigDecimal("1.05");
    private static final BigDecimal VOLATILITY_STD_DEV_THRESHOLD = new BigDecimal("0.3");
    private static final BigDecimal SEASONAL_DEVIATION_THRESHOLD_PERCENT = new BigDecimal("25");

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
        runVolatilityRule(loan, statement);
        runSeasonalAnomalyRule(loan, statement);
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
            BigDecimal actual = financialRatioService.calculateByCovenantType(covenant.getType(), statement);

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

    private void runVolatilityRule(Loan loan, FinancialStatement statement) {
        List<FinancialStatement> statements = financialStatementRepository
                .findByLoanIdAndPeriodTypeAndSupersededFalseOrderByFiscalYearAscFiscalQuarterAscIdAsc(
                        loan.getId(),
                        statement.getPeriodType()
                );

        if (statements.size() < 4) {
            return;
        }

        List<FinancialStatement> lastFour = statements.subList(statements.size() - 4, statements.size());
        BigDecimal standardDeviation = calculateStandardDeviation(lastFour.stream()
                .map(financialRatioService::calculateCurrentRatio)
                .map(BigDecimal::doubleValue)
                .toList());

        if (standardDeviation.compareTo(VOLATILITY_STD_DEV_THRESHOLD) > 0) {
            alertService.createAlert(
                    loan,
                    statement,
                    AlertType.EARLY_WARNING,
                    "Current ratio volatility detected. StdDev=" + standardDeviation,
                    SeverityLevel.MEDIUM,
                    "CURRENT_RATIO_VOLATILITY"
            );
        }
    }

    private void runSeasonalAnomalyRule(Loan loan, FinancialStatement statement) {
        if (statement.getPeriodType() != PeriodType.QUARTERLY || statement.getFiscalQuarter() == null) {
            return;
        }

        List<FinancialStatement> statements = financialStatementRepository
                .findByLoanIdAndPeriodTypeAndSupersededFalseOrderByFiscalYearAscFiscalQuarterAscIdAsc(
                        loan.getId(),
                        PeriodType.QUARTERLY
                );

        if (statements.size() < 4) {
            return;
        }

        Optional<FinancialStatement> previousYearSameQuarter = statements.stream()
                .filter(s -> s.getFiscalQuarter() != null
                        && s.getFiscalQuarter().equals(statement.getFiscalQuarter())
                        && s.getFiscalYear().equals(statement.getFiscalYear() - 1))
                .findFirst();

        if (previousYearSameQuarter.isEmpty()) {
            return;
        }

        BigDecimal currentRatio = financialRatioService.calculateCurrentRatio(statement);
        BigDecimal previousRatio = financialRatioService.calculateCurrentRatio(previousYearSameQuarter.get());
        if (previousRatio.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        BigDecimal deviationPercent = currentRatio.subtract(previousRatio)
                .abs()
                .divide(previousRatio.abs(), 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        if (deviationPercent.compareTo(SEASONAL_DEVIATION_THRESHOLD_PERCENT) > 0) {
            alertService.createAlert(
                    loan,
                    statement,
                    AlertType.EARLY_WARNING,
                    "Seasonal anomaly detected for current ratio. Deviation=" + deviationPercent + "%",
                    SeverityLevel.LOW,
                    "CURRENT_RATIO_SEASONAL_ANOMALY"
            );
        }
    }

    private BigDecimal calculateStandardDeviation(List<Double> values) {
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0D);
        return BigDecimal.valueOf(Math.sqrt(variance)).setScale(4, RoundingMode.HALF_UP);
    }
}
