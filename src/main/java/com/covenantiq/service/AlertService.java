package com.covenantiq.service;

import com.covenantiq.domain.Alert;
import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.domain.Loan;
import com.covenantiq.enums.AlertType;
import com.covenantiq.enums.SeverityLevel;
import com.covenantiq.repository.AlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Transactional
    public Alert createAlert(
            Loan loan,
            FinancialStatement statement,
            AlertType alertType,
            String message,
            SeverityLevel severityLevel,
            String alertRuleCode
    ) {
        Alert alert = new Alert();
        alert.setLoan(loan);
        alert.setFinancialStatement(statement);
        alert.setAlertType(alertType);
        alert.setMessage(message);
        alert.setSeverityLevel(severityLevel);
        alert.setAlertRuleCode(alertRuleCode);
        alert.setTriggeredTimestampUtc(OffsetDateTime.now(java.time.ZoneOffset.UTC));
        return alertRepository.save(alert);
    }
}
