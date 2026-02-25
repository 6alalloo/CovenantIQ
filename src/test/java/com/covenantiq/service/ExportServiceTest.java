package com.covenantiq.service;

import com.covenantiq.domain.Alert;
import com.covenantiq.domain.Covenant;
import com.covenantiq.domain.CovenantResult;
import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.domain.Loan;
import com.covenantiq.enums.AlertStatus;
import com.covenantiq.enums.ComparisonType;
import com.covenantiq.enums.CovenantResultStatus;
import com.covenantiq.enums.CovenantType;
import com.covenantiq.enums.SeverityLevel;
import com.covenantiq.repository.AlertRepository;
import com.covenantiq.repository.CovenantResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private CovenantResultRepository covenantResultRepository;

    @Mock
    private LoanService loanService;

    @InjectMocks
    private ExportService exportService;

    @Test
    void exportAlertsCsvIncludesExpectedHeaderAndEscapedFields() {
        Long loanId = 1L;
        Loan loan = loan(loanId, "ACME, \"Corp\"");
        Alert alert = new Alert();
        alert.setId(10L);
        alert.setLoan(loan);
        alert.setSeverityLevel(SeverityLevel.HIGH);
        alert.setMessage("Needs \"urgent\", review");
        alert.setStatus(AlertStatus.OPEN);
        alert.setTriggeredTimestampUtc(OffsetDateTime.of(2026, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC));

        when(loanService.getLoan(loanId)).thenReturn(loan);
        when(alertRepository.findByLoanIdAndSupersededFalseOrderByTriggeredTimestampUtcDescIdDesc(loanId))
                .thenReturn(List.of(alert));

        String csv = exportService.exportAlertsCsv(loanId);

        assertTrue(csv.startsWith("Alert ID,Loan ID,Loan Name,Severity,Message,Status,Created At,Acknowledged By,Acknowledged At,Resolved By,Resolved At,Resolution Notes"));
        assertTrue(csv.contains("\"ACME, \"\"Corp\"\"\""));
        assertTrue(csv.contains("\"Needs \"\"urgent\"\", review\""));
        assertTrue(csv.contains(",OPEN,"));
        verify(loanService).getLoan(loanId);
    }

    @Test
    void exportCovenantResultsCsvFormatsDecimalsAndPeriodSuffix() {
        Long loanId = 2L;
        Loan loan = loan(loanId, "Borrower");

        FinancialStatement statement = new FinancialStatement();
        statement.setFiscalYear(2025);
        statement.setFiscalQuarter(3);

        Covenant covenant = new Covenant();
        covenant.setType(CovenantType.CURRENT_RATIO);
        covenant.setThresholdValue(new BigDecimal("1.2"));
        covenant.setComparisonType(ComparisonType.GREATER_THAN_EQUAL);

        CovenantResult result = new CovenantResult();
        result.setEvaluationTimestampUtc(OffsetDateTime.of(2026, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC));
        result.setCovenant(covenant);
        result.setActualValue(new BigDecimal("1.23456"));
        result.setStatus(CovenantResultStatus.PASS);
        result.setFinancialStatement(statement);

        when(loanService.getLoan(loanId)).thenReturn(loan);
        when(covenantResultRepository.findByFinancialStatementLoanIdAndSupersededFalseOrderByEvaluationTimestampUtcDescIdDesc(loanId))
                .thenReturn(List.of(result));

        String csv = exportService.exportCovenantResultsCsv(loanId);

        assertTrue(csv.startsWith("Evaluation Date,Covenant Name,Covenant Type,Calculated Value,Threshold Value,Comparison Operator,Compliance Status,Statement Period End Date"));
        assertTrue(csv.contains("1.2346"));
        assertTrue(csv.contains("1.2000"));
        assertTrue(csv.contains("GREATER_THAN_EQUAL"));
        assertTrue(csv.contains("PASS"));
        assertTrue(csv.contains("\"2025-Q3\""));
        verify(loanService).getLoan(loanId);
    }

    private Loan loan(Long id, String borrowerName) {
        Loan loan = new Loan();
        loan.setId(id);
        loan.setBorrowerName(borrowerName);
        return loan;
    }
}
