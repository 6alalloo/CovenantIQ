package com.covenantiq.service;

import com.covenantiq.domain.Covenant;
import com.covenantiq.domain.CovenantResult;
import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.domain.Loan;
import com.covenantiq.enums.ComparisonType;
import com.covenantiq.enums.CovenantResultStatus;
import com.covenantiq.enums.CovenantType;
import com.covenantiq.enums.SeverityLevel;
import com.covenantiq.repository.CovenantRepository;
import com.covenantiq.repository.CovenantResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CovenantEvaluationServiceTest {

    @Mock
    private CovenantRepository covenantRepository;

    @Mock
    private CovenantResultRepository covenantResultRepository;

    @Mock
    private AlertService alertService;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @Mock
    private CollateralExceptionService collateralExceptionService;

    private CovenantEvaluationService covenantEvaluationService;

    @BeforeEach
    void setUp() {
        covenantEvaluationService = new CovenantEvaluationService(
                covenantRepository,
                covenantResultRepository,
                new FinancialRatioService(),
                alertService,
                outboxEventPublisher,
                collateralExceptionService
        );
        when(collateralExceptionService.getActiveApprovedException(any(), any())).thenReturn(java.util.Optional.empty());
    }

    @Test
    void createsPassAndBreachResultsAndAlert() {
        Loan loan = new Loan();
        loan.setId(1L);

        Covenant currentRatio = new Covenant();
        currentRatio.setId(11L);
        currentRatio.setLoan(loan);
        currentRatio.setType(CovenantType.CURRENT_RATIO);
        currentRatio.setComparisonType(ComparisonType.GREATER_THAN_EQUAL);
        currentRatio.setThresholdValue(new BigDecimal("1.20"));
        currentRatio.setSeverityLevel(SeverityLevel.HIGH);

        Covenant debtToEquity = new Covenant();
        debtToEquity.setId(12L);
        debtToEquity.setLoan(loan);
        debtToEquity.setType(CovenantType.DEBT_TO_EQUITY);
        debtToEquity.setComparisonType(ComparisonType.LESS_THAN_EQUAL);
        debtToEquity.setThresholdValue(new BigDecimal("2.00"));
        debtToEquity.setSeverityLevel(SeverityLevel.MEDIUM);

        FinancialStatement statement = new FinancialStatement();
        statement.setId(100L);
        statement.setCurrentAssets(new BigDecimal("140"));
        statement.setCurrentLiabilities(new BigDecimal("100"));
        statement.setTotalDebt(new BigDecimal("300"));
        statement.setTotalEquity(new BigDecimal("100"));

        when(covenantRepository.findByLoanIdOrderByIdAsc(loan.getId())).thenReturn(List.of(currentRatio, debtToEquity));
        when(covenantResultRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<CovenantResult> results = covenantEvaluationService.evaluateAndPersist(loan, statement);

        assertEquals(2, results.size());
        assertEquals(CovenantResultStatus.PASS, results.get(0).getStatus());
        assertEquals(CovenantResultStatus.BREACH, results.get(1).getStatus());
        verify(alertService, times(1)).createAlert(any(), any(), any(), any(), any(), any());
    }
}
