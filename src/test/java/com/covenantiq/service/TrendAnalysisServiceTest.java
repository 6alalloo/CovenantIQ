package com.covenantiq.service;

import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.domain.Loan;
import com.covenantiq.enums.PeriodType;
import com.covenantiq.repository.CovenantRepository;
import com.covenantiq.repository.FinancialStatementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrendAnalysisServiceTest {

    @Mock
    private FinancialStatementRepository financialStatementRepository;

    @Mock
    private CovenantRepository covenantRepository;

    @Mock
    private AlertService alertService;

    private TrendAnalysisService trendAnalysisService;

    @BeforeEach
    void setUp() {
        trendAnalysisService = new TrendAnalysisService(
                financialStatementRepository,
                covenantRepository,
                new FinancialRatioService(),
                alertService
        );
    }

    @Test
    void createsVolatilityAlertWhenStdDevExceedsThreshold() {
        Loan loan = loan(10L);
        FinancialStatement q1 = quarterly(2024, 1, "60", "100");
        FinancialStatement q2 = quarterly(2024, 2, "100", "100");
        FinancialStatement q3 = quarterly(2024, 3, "140", "100");
        FinancialStatement q4 = quarterly(2024, 4, "220", "100");

        when(financialStatementRepository.findByLoanIdAndPeriodTypeAndSupersededFalseOrderByFiscalYearAscFiscalQuarterAscIdAsc(
                loan.getId(), PeriodType.QUARTERLY)).thenReturn(List.of(q1, q2, q3, q4));
        when(covenantRepository.findByLoanIdOrderByIdAsc(loan.getId())).thenReturn(List.of());

        trendAnalysisService.evaluateAndPersist(loan, q4);

        verify(alertService).createAlert(any(), any(), any(), any(), any(), eq("CURRENT_RATIO_VOLATILITY"));
    }

    @Test
    void skipsVolatilityWhenLessThanFourStatements() {
        Loan loan = loan(11L);
        FinancialStatement q1 = quarterly(2025, 1, "100", "100");
        FinancialStatement q2 = quarterly(2025, 2, "105", "100");
        FinancialStatement q3 = quarterly(2025, 3, "110", "100");

        when(financialStatementRepository.findByLoanIdAndPeriodTypeAndSupersededFalseOrderByFiscalYearAscFiscalQuarterAscIdAsc(
                loan.getId(), PeriodType.QUARTERLY)).thenReturn(List.of(q1, q2, q3));
        when(covenantRepository.findByLoanIdOrderByIdAsc(loan.getId())).thenReturn(List.of());

        trendAnalysisService.evaluateAndPersist(loan, q3);

        verify(alertService, never()).createAlert(any(), any(), any(), any(), any(), eq("CURRENT_RATIO_VOLATILITY"));
    }

    @Test
    void createsSeasonalAnomalyAlertWhenDeviationExceedsThreshold() {
        Loan loan = loan(12L);
        FinancialStatement q1_2024 = quarterly(2024, 1, "60", "100");
        FinancialStatement q2_2024 = quarterly(2024, 2, "100", "100");
        FinancialStatement q3_2024 = quarterly(2024, 3, "140", "100");
        FinancialStatement q1_2025 = quarterly(2025, 1, "220", "100");

        when(financialStatementRepository.findByLoanIdAndPeriodTypeAndSupersededFalseOrderByFiscalYearAscFiscalQuarterAscIdAsc(
                loan.getId(), PeriodType.QUARTERLY)).thenReturn(List.of(q1_2024, q2_2024, q3_2024, q1_2025));
        when(covenantRepository.findByLoanIdOrderByIdAsc(loan.getId())).thenReturn(List.of());

        trendAnalysisService.evaluateAndPersist(loan, q1_2025);

        verify(alertService).createAlert(any(), any(), any(), any(), any(), eq("CURRENT_RATIO_SEASONAL_ANOMALY"));
    }

    @Test
    void skipsSeasonalAnomalyForAnnualStatements() {
        Loan loan = loan(13L);
        FinancialStatement annual = annual(2025, "200", "100");

        when(financialStatementRepository.findByLoanIdAndPeriodTypeAndSupersededFalseOrderByFiscalYearAscFiscalQuarterAscIdAsc(
                loan.getId(), PeriodType.ANNUAL)).thenReturn(List.of(annual, annual, annual, annual));
        when(covenantRepository.findByLoanIdOrderByIdAsc(loan.getId())).thenReturn(List.of());

        trendAnalysisService.evaluateAndPersist(loan, annual);

        verify(alertService, never()).createAlert(any(), any(), any(), any(), any(), eq("CURRENT_RATIO_SEASONAL_ANOMALY"));
    }

    private Loan loan(Long id) {
        Loan loan = new Loan();
        loan.setId(id);
        return loan;
    }

    private FinancialStatement quarterly(int year, int quarter, String assets, String liabilities) {
        FinancialStatement statement = new FinancialStatement();
        statement.setPeriodType(PeriodType.QUARTERLY);
        statement.setFiscalYear(year);
        statement.setFiscalQuarter(quarter);
        statement.setCurrentAssets(new BigDecimal(assets));
        statement.setCurrentLiabilities(new BigDecimal(liabilities));
        return statement;
    }

    private FinancialStatement annual(int year, String assets, String liabilities) {
        FinancialStatement statement = new FinancialStatement();
        statement.setPeriodType(PeriodType.ANNUAL);
        statement.setFiscalYear(year);
        statement.setCurrentAssets(new BigDecimal(assets));
        statement.setCurrentLiabilities(new BigDecimal(liabilities));
        return statement;
    }
}
