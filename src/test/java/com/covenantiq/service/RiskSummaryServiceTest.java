package com.covenantiq.service;

import com.covenantiq.domain.Alert;
import com.covenantiq.domain.Covenant;
import com.covenantiq.domain.CovenantResult;
import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.dto.response.RiskDetailsResponse;
import com.covenantiq.dto.response.RiskSummaryResponse;
import com.covenantiq.enums.AlertStatus;
import com.covenantiq.enums.AlertType;
import com.covenantiq.enums.CovenantResultStatus;
import com.covenantiq.enums.CovenantType;
import com.covenantiq.enums.RiskLevel;
import com.covenantiq.enums.SeverityLevel;
import com.covenantiq.repository.AlertRepository;
import com.covenantiq.repository.CovenantRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskSummaryServiceTest {

    @Mock
    private FinancialStatementService financialStatementService;

    @Mock
    private CovenantRepository covenantRepository;

    @Mock
    private CovenantResultRepository covenantResultRepository;

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private RiskSummaryService riskSummaryService;

    @Test
    void getRiskSummaryReturnsLowWhenNoStatementsExist() {
        when(covenantRepository.findByLoanIdOrderByIdAsc(1L)).thenReturn(List.of(new Covenant(), new Covenant()));
        when(financialStatementService.getLatestStatement(1L)).thenReturn(null);

        RiskSummaryResponse response = riskSummaryService.getRiskSummary(1L);

        assertEquals(2, response.totalCovenants());
        assertEquals(0, response.breachedCount());
        assertEquals(0, response.activeWarnings());
        assertEquals(RiskLevel.LOW, response.overallRiskLevel());
    }

    @Test
    void getRiskSummaryReturnsHighWhenAnyHighSeverityBreachExists() {
        FinancialStatement latest = statement(100L);
        when(covenantRepository.findByLoanIdOrderByIdAsc(1L)).thenReturn(List.of(new Covenant()));
        when(financialStatementService.getLatestStatement(1L)).thenReturn(latest);
        when(covenantResultRepository.countByFinancialStatementIdAndStatusAndSupersededFalse(100L, CovenantResultStatus.BREACH))
                .thenReturn(1L);
        when(alertRepository.countByFinancialStatementIdAndAlertTypeAndStatusNotAndSupersededFalse(
                100L, AlertType.EARLY_WARNING, AlertStatus.RESOLVED
        )).thenReturn(0L);
        when(alertRepository.existsByFinancialStatementIdAndAlertTypeAndSeverityLevelAndSupersededFalse(
                100L, AlertType.BREACH, SeverityLevel.HIGH
        )).thenReturn(true);

        RiskSummaryResponse response = riskSummaryService.getRiskSummary(1L);

        assertEquals(RiskLevel.HIGH, response.overallRiskLevel());
    }

    @Test
    void getRiskSummaryReturnsMediumWhenNoHighBreachButWarningsExist() {
        FinancialStatement latest = statement(101L);
        when(covenantRepository.findByLoanIdOrderByIdAsc(1L)).thenReturn(List.of(new Covenant()));
        when(financialStatementService.getLatestStatement(1L)).thenReturn(latest);
        when(covenantResultRepository.countByFinancialStatementIdAndStatusAndSupersededFalse(101L, CovenantResultStatus.BREACH))
                .thenReturn(0L);
        when(alertRepository.countByFinancialStatementIdAndAlertTypeAndStatusNotAndSupersededFalse(
                101L, AlertType.EARLY_WARNING, AlertStatus.RESOLVED
        )).thenReturn(2L);
        when(alertRepository.existsByFinancialStatementIdAndAlertTypeAndSeverityLevelAndSupersededFalse(
                101L, AlertType.BREACH, SeverityLevel.HIGH
        )).thenReturn(false);

        RiskSummaryResponse response = riskSummaryService.getRiskSummary(1L);

        assertEquals(RiskLevel.MEDIUM, response.overallRiskLevel());
    }

    @Test
    void getRiskSummaryReturnsLowWhenNoBreachesAndNoWarnings() {
        FinancialStatement latest = statement(102L);
        when(covenantRepository.findByLoanIdOrderByIdAsc(1L)).thenReturn(List.of(new Covenant()));
        when(financialStatementService.getLatestStatement(1L)).thenReturn(latest);
        when(covenantResultRepository.countByFinancialStatementIdAndStatusAndSupersededFalse(102L, CovenantResultStatus.BREACH))
                .thenReturn(0L);
        when(alertRepository.countByFinancialStatementIdAndAlertTypeAndStatusNotAndSupersededFalse(
                102L, AlertType.EARLY_WARNING, AlertStatus.RESOLVED
        )).thenReturn(0L);
        when(alertRepository.existsByFinancialStatementIdAndAlertTypeAndSeverityLevelAndSupersededFalse(
                102L, AlertType.BREACH, SeverityLevel.HIGH
        )).thenReturn(false);

        RiskSummaryResponse response = riskSummaryService.getRiskSummary(1L);

        assertEquals(RiskLevel.LOW, response.overallRiskLevel());
    }

    @Test
    void getRiskDetailsReturnsEmptyPayloadWhenNoStatementsExist() {
        when(financialStatementService.getLatestStatement(7L)).thenReturn(null);

        RiskDetailsResponse response = riskSummaryService.getRiskDetails(7L);

        assertEquals(7L, response.loanId());
        assertNull(response.financialStatementId());
        assertNull(response.evaluationTimestampUtc());
        assertEquals(0, response.details().size());
    }

    @Test
    void getRiskDetailsMapsCovenantsAndTriggeredRuleCodes() {
        FinancialStatement latest = statement(300L);

        Covenant covenant = new Covenant();
        covenant.setId(10L);
        covenant.setType(CovenantType.CURRENT_RATIO);
        covenant.setThresholdValue(new BigDecimal("1.2000"));
        covenant.setSeverityLevel(SeverityLevel.HIGH);

        CovenantResult result = new CovenantResult();
        result.setId(20L);
        result.setCovenant(covenant);
        result.setActualValue(new BigDecimal("1.1000"));
        result.setStatus(CovenantResultStatus.BREACH);

        Alert matchingByMessage = new Alert();
        matchingByMessage.setAlertRuleCode("RULE_A");
        matchingByMessage.setMessage("CURRENT_RATIO breached");

        Alert matchingByRuleCode = new Alert();
        matchingByRuleCode.setAlertRuleCode("RULE_CURRENT_RATIO_WARNING");
        matchingByRuleCode.setMessage("Near threshold");

        Alert unrelated = new Alert();
        unrelated.setAlertRuleCode("RULE_OTHER");
        unrelated.setMessage("Other covenant");

        when(financialStatementService.getLatestStatement(1L)).thenReturn(latest);
        when(covenantResultRepository.findByFinancialStatementIdAndSupersededFalseOrderByEvaluationTimestampUtcDescIdDesc(300L))
                .thenReturn(List.of(result));
        when(alertRepository.findByFinancialStatementIdAndSupersededFalseOrderByTriggeredTimestampUtcDescIdDesc(300L))
                .thenReturn(List.of(matchingByMessage, matchingByRuleCode, unrelated));

        RiskDetailsResponse response = riskSummaryService.getRiskDetails(1L);

        assertEquals(1, response.details().size());
        assertEquals(10L, response.details().get(0).covenantId());
        assertEquals(CovenantType.CURRENT_RATIO, response.details().get(0).covenantType());
        assertEquals(2, response.details().get(0).triggeredRuleCodes().size());
        assertEquals("RULE_A", response.details().get(0).triggeredRuleCodes().get(0));
        assertEquals("RULE_CURRENT_RATIO_WARNING", response.details().get(0).triggeredRuleCodes().get(1));
    }

    private FinancialStatement statement(Long id) {
        FinancialStatement statement = new FinancialStatement();
        statement.setId(id);
        statement.setSubmissionTimestampUtc(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        return statement;
    }
}
