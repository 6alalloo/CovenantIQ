package com.covenantiq.service;

import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.domain.Loan;
import com.covenantiq.dto.response.PortfolioSummaryResponse;
import com.covenantiq.dto.response.PortfolioTrendPointResponse;
import com.covenantiq.enums.AlertStatus;
import com.covenantiq.enums.CovenantResultStatus;
import com.covenantiq.enums.LoanStatus;
import com.covenantiq.repository.AlertRepository;
import com.covenantiq.repository.CovenantResultRepository;
import com.covenantiq.repository.FinancialStatementRepository;
import com.covenantiq.repository.LoanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioSummaryServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private FinancialStatementService financialStatementService;

    @Mock
    private CovenantResultRepository covenantResultRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private FinancialStatementRepository financialStatementRepository;

    @InjectMocks
    private PortfolioSummaryService portfolioSummaryService;

    @Test
    void getSummaryAggregatesActiveLoanRiskAndAlertCounts() {
        Loan activeHighRisk = loan(1L, LoanStatus.ACTIVE);
        Loan activeMediumRisk = loan(2L, LoanStatus.ACTIVE);
        Loan activeLowRiskNoStatements = loan(3L, LoanStatus.ACTIVE);
        Loan closedLoan = loan(4L, LoanStatus.CLOSED);
        when(loanRepository.findAll()).thenReturn(List.of(activeHighRisk, activeMediumRisk, activeLowRiskNoStatements, closedLoan));

        FinancialStatement statement1 = statement(101L);
        FinancialStatement statement2 = statement(102L);

        when(financialStatementService.getLatestStatement(1L)).thenReturn(statement1);
        when(financialStatementService.getLatestStatement(2L)).thenReturn(statement2);
        when(financialStatementService.getLatestStatement(3L)).thenReturn(null);

        when(covenantResultRepository.countByFinancialStatementIdAndStatusAndSupersededFalse(101L, CovenantResultStatus.BREACH))
                .thenReturn(2L);
        when(covenantResultRepository.countByFinancialStatementIdAndStatusAndSupersededFalse(102L, CovenantResultStatus.BREACH))
                .thenReturn(1L);

        when(alertRepository.countBySupersededFalseAndStatusIn(EnumSet.of(AlertStatus.OPEN))).thenReturn(5L);
        when(alertRepository.countBySupersededFalseAndStatusIn(EnumSet.of(AlertStatus.UNDER_REVIEW))).thenReturn(2L);

        PortfolioSummaryResponse response = portfolioSummaryService.getSummary();

        assertEquals(3, response.totalActiveLoans());
        assertEquals(3, response.totalBreaches());
        assertEquals(1, response.highRiskLoanCount());
        assertEquals(1, response.mediumRiskLoanCount());
        assertEquals(1, response.lowRiskLoanCount());
        assertEquals(5, response.totalOpenAlerts());
        assertEquals(2, response.totalUnderReviewAlerts());
    }

    @Test
    void getTrendBuildsQuarterlyPortfolioRiskSeriesFromHistoricalStatements() {
        Loan activeHighRisk = loan(1L, LoanStatus.ACTIVE);
        Loan activeMediumRisk = loan(2L, LoanStatus.ACTIVE);
        Loan activeLateStarter = loan(3L, LoanStatus.ACTIVE);
        when(loanRepository.findAll()).thenReturn(List.of(activeHighRisk, activeMediumRisk, activeLateStarter));

        FinancialStatement q4_2025_high = statement(101L, activeHighRisk, ts(2025, 12, 31));
        FinancialStatement q1_2026_high = statement(102L, activeHighRisk, ts(2026, 3, 31));
        FinancialStatement q4_2025_medium = statement(201L, activeMediumRisk, ts(2025, 12, 31));
        FinancialStatement q1_2026_low = statement(202L, activeMediumRisk, ts(2026, 3, 31));
        FinancialStatement q1_2026_newMedium = statement(301L, activeLateStarter, ts(2026, 3, 31));

        when(financialStatementRepository.findByLoanIdInAndSupersededFalseOrderBySubmissionTimestampUtcAscIdAsc(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(q4_2025_high, q4_2025_medium, q1_2026_high, q1_2026_low, q1_2026_newMedium));

        when(covenantResultRepository.findByFinancialStatementIdInAndSupersededFalse(List.of(101L, 201L, 102L, 202L, 301L)))
                .thenReturn(List.of(
                        breachResult(q4_2025_high), breachResult(q4_2025_high),
                        breachResult(q4_2025_medium),
                        breachResult(q1_2026_high), breachResult(q1_2026_high),
                        breachResult(q1_2026_newMedium)
                ));

        List<PortfolioTrendPointResponse> trend = portfolioSummaryService.getTrend();

        assertEquals(2, trend.size());
        assertEquals("2025 Q4", trend.get(0).periodLabel());
        assertEquals(1, trend.get(0).highRiskLoanCount());
        assertEquals(1, trend.get(0).mediumRiskLoanCount());
        assertEquals(0, trend.get(0).lowRiskLoanCount());
        assertEquals("2026 Q1", trend.get(1).periodLabel());
        assertEquals(1, trend.get(1).highRiskLoanCount());
        assertEquals(1, trend.get(1).mediumRiskLoanCount());
        assertEquals(1, trend.get(1).lowRiskLoanCount());
    }

    private Loan loan(Long id, LoanStatus status) {
        Loan loan = new Loan();
        loan.setId(id);
        loan.setStatus(status);
        return loan;
    }

    private FinancialStatement statement(Long id) {
        FinancialStatement statement = new FinancialStatement();
        statement.setId(id);
        return statement;
    }

    private FinancialStatement statement(Long id, Loan loan, OffsetDateTime submissionTimestampUtc) {
        FinancialStatement statement = statement(id);
        statement.setLoan(loan);
        statement.setSubmissionTimestampUtc(submissionTimestampUtc);
        return statement;
    }

    private com.covenantiq.domain.CovenantResult breachResult(FinancialStatement statement) {
        com.covenantiq.domain.CovenantResult result = new com.covenantiq.domain.CovenantResult();
        result.setFinancialStatement(statement);
        result.setStatus(CovenantResultStatus.BREACH);
        return result;
    }

    private OffsetDateTime ts(int year, int month, int dayOfMonth) {
        return OffsetDateTime.of(year, month, dayOfMonth, 12, 0, 0, 0, ZoneOffset.UTC);
    }
}
