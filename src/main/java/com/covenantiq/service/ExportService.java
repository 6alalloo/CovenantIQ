package com.covenantiq.service;

import com.covenantiq.domain.Alert;
import com.covenantiq.domain.CovenantResult;
import com.covenantiq.repository.AlertRepository;
import com.covenantiq.repository.CovenantResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final AlertRepository alertRepository;
    private final CovenantResultRepository covenantResultRepository;
    private final LoanService loanService;

    public ExportService(
            AlertRepository alertRepository,
            CovenantResultRepository covenantResultRepository,
            LoanService loanService
    ) {
        this.alertRepository = alertRepository;
        this.covenantResultRepository = covenantResultRepository;
        this.loanService = loanService;
    }

    @Transactional(readOnly = true)
    public String exportAlertsCsv(Long loanId) {
        loanService.getLoan(loanId);
        List<Alert> alerts = alertRepository.findByLoanIdAndSupersededFalseOrderByTriggeredTimestampUtcDescIdDesc(loanId);
        StringBuilder sb = new StringBuilder();
        sb.append("Alert ID,Loan ID,Loan Name,Severity,Message,Status,Created At,Acknowledged By,Acknowledged At,Resolved By,Resolved At,Resolution Notes\n");
        for (Alert alert : alerts) {
            sb.append(alert.getId()).append(",")
                    .append(alert.getLoan().getId()).append(",")
                    .append(csv(alert.getLoan().getBorrowerName())).append(",")
                    .append(alert.getSeverityLevel()).append(",")
                    .append(csv(alert.getMessage())).append(",")
                    .append(alert.getStatus()).append(",")
                    .append(csv(ISO.format(alert.getTriggeredTimestampUtc()))).append(",")
                    .append(csv(alert.getAcknowledgedBy())).append(",")
                    .append(csv(formatNullable(alert.getAcknowledgedAt()))).append(",")
                    .append(csv(alert.getResolvedBy())).append(",")
                    .append(csv(formatNullable(alert.getResolvedAt()))).append(",")
                    .append(csv(alert.getResolutionNotes()))
                    .append("\n");
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String exportCovenantResultsCsv(Long loanId) {
        loanService.getLoan(loanId);
        List<CovenantResult> results =
                covenantResultRepository.findByFinancialStatementLoanIdAndSupersededFalseOrderByEvaluationTimestampUtcDescIdDesc(
                        loanId
                );
        StringBuilder sb = new StringBuilder();
        sb.append("Evaluation Date,Covenant Name,Covenant Type,Calculated Value,Threshold Value,Comparison Operator,Compliance Status,Statement Period End Date\n");
        for (CovenantResult result : results) {
            sb.append(csv(ISO.format(result.getEvaluationTimestampUtc()))).append(",")
                    .append(csv(result.getCovenant().getType().name())).append(",")
                    .append(result.getCovenant().getType()).append(",")
                    .append(result.getActualValue().setScale(4, RoundingMode.HALF_UP)).append(",")
                    .append(result.getCovenant().getThresholdValue().setScale(4, RoundingMode.HALF_UP)).append(",")
                    .append(result.getCovenant().getComparisonType()).append(",")
                    .append(result.getStatus()).append(",")
                    .append(csv(result.getFinancialStatement().getFiscalYear()
                            + periodSuffix(result.getFinancialStatement().getFiscalQuarter())))
                    .append("\n");
        }
        return sb.toString();
    }

    private String periodSuffix(Integer quarter) {
        if (quarter == null) {
            return "";
        }
        return "-Q" + quarter;
    }

    private String formatNullable(java.time.OffsetDateTime value) {
        return value == null ? "" : ISO.format(value);
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
