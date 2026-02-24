package com.covenantiq.mapper;

import com.covenantiq.domain.Alert;
import com.covenantiq.domain.Covenant;
import com.covenantiq.domain.CovenantResult;
import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.domain.Loan;
import com.covenantiq.dto.response.AlertResponse;
import com.covenantiq.dto.response.CovenantResponse;
import com.covenantiq.dto.response.CovenantResultResponse;
import com.covenantiq.dto.response.FinancialStatementResponse;
import com.covenantiq.dto.response.LoanResponse;

public final class ResponseMapper {

    private ResponseMapper() {
    }

    public static LoanResponse toLoanResponse(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getBorrowerName(),
                loan.getPrincipalAmount(),
                loan.getStartDate(),
                loan.getStatus()
        );
    }

    public static CovenantResponse toCovenantResponse(Covenant covenant) {
        return new CovenantResponse(
                covenant.getId(),
                covenant.getLoan().getId(),
                covenant.getType(),
                covenant.getThresholdValue(),
                covenant.getComparisonType(),
                covenant.getSeverityLevel()
        );
    }

    public static FinancialStatementResponse toFinancialStatementResponse(FinancialStatement statement) {
        return new FinancialStatementResponse(
                statement.getId(),
                statement.getLoan().getId(),
                statement.getPeriodType(),
                statement.getFiscalYear(),
                statement.getFiscalQuarter(),
                statement.getCurrentAssets(),
                statement.getCurrentLiabilities(),
                statement.getTotalDebt(),
                statement.getTotalEquity(),
                statement.getEbit(),
                statement.getInterestExpense(),
                statement.getSubmissionTimestampUtc()
        );
    }

    public static CovenantResultResponse toCovenantResultResponse(CovenantResult result) {
        return new CovenantResultResponse(
                result.getId(),
                result.getCovenant().getId(),
                result.getCovenant().getType(),
                result.getFinancialStatement().getId(),
                result.getActualValue(),
                result.getStatus(),
                result.getEvaluationTimestampUtc()
        );
    }

    public static AlertResponse toAlertResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getLoan().getId(),
                alert.getFinancialStatement().getId(),
                alert.getAlertType(),
                alert.getMessage(),
                alert.getSeverityLevel(),
                alert.getAlertRuleCode(),
                alert.getTriggeredTimestampUtc()
        );
    }
}
