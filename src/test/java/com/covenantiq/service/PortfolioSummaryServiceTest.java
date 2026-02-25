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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
