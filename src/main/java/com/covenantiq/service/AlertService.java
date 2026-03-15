package com.covenantiq.service;

import com.covenantiq.domain.Alert;
import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.domain.Loan;
import com.covenantiq.enums.ActivityEventType;
import com.covenantiq.enums.AlertStatus;
import com.covenantiq.enums.AlertType;
import com.covenantiq.enums.SeverityLevel;
import com.covenantiq.exception.ConflictException;
import com.covenantiq.exception.ForbiddenOperationException;
import com.covenantiq.exception.ResourceNotFoundException;
import com.covenantiq.exception.UnprocessableEntityException;
import com.covenantiq.repository.AlertRepository;
import com.covenantiq.security.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final CurrentUserService currentUserService;
    private final ActivityLogService activityLogService;
    private final OutboxEventPublisher outboxEventPublisher;

    public AlertService(
            AlertRepository alertRepository,
            CurrentUserService currentUserService,
            ActivityLogService activityLogService,
            OutboxEventPublisher outboxEventPublisher
    ) {
        this.alertRepository = alertRepository;
        this.currentUserService = currentUserService;
        this.activityLogService = activityLogService;
        this.outboxEventPublisher = outboxEventPublisher;
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
        alert.setTriggeredTimestampUtc(OffsetDateTime.now(ZoneOffset.UTC));
        Alert saved = alertRepository.save(alert);
        outboxEventPublisher.publish("Alert", saved.getId(), "AlertCreated", java.util.Map.of(
                "alertId", saved.getId(),
                "loanId", loan.getId(),
                "status", saved.getStatus().name(),
                "severity", saved.getSeverityLevel().name(),
                "alertType", saved.getAlertType().name(),
                "eventTime", saved.getTriggeredTimestampUtc().toString()
        ));
        return saved;
    }

    @Transactional
    public Alert updateStatus(Long alertId, AlertStatus targetStatus, String resolutionNotes) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert " + alertId + " not found"));
        if (alert.isSuperseded()) {
            throw new ConflictException("Cannot update superseded alert");
        }
        if (alert.getStatus() == targetStatus) {
            return alert;
        }
        AlertStatus previousStatus = alert.getStatus();

        if (targetStatus == AlertStatus.RESOLVED && (resolutionNotes == null || resolutionNotes.isBlank())) {
            throw new UnprocessableEntityException("resolutionNotes is required when resolving an alert");
        }

        validateTransition(alert.getStatus(), targetStatus);
        validateRole(alert.getStatus(), targetStatus);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String username = currentUserService.usernameOrSystem();
        switch (targetStatus) {
            case ACKNOWLEDGED -> {
                alert.setAcknowledgedBy(username);
                alert.setAcknowledgedAt(now);
            }
            case RESOLVED -> {
                alert.setResolvedBy(username);
                alert.setResolvedAt(now);
                alert.setResolutionNotes(resolutionNotes.trim());
            }
            case UNDER_REVIEW, OPEN -> {
                // no additional fields
            }
        }
        alert.setStatus(targetStatus);
        Alert saved = alertRepository.save(alert);
        log.info("Alert status updated: alertId={}, from={}, to={}, by={}",
                alertId, previousStatus, targetStatus, username);
        outboxEventPublisher.publish("Alert", saved.getId(), "AlertStatusChanged", java.util.Map.of(
                "alertId", saved.getId(),
                "loanId", saved.getLoan().getId(),
                "fromStatus", previousStatus.name(),
                "toStatus", targetStatus.name(),
                "actor", username,
                "severity", saved.getSeverityLevel() == null ? "UNKNOWN" : saved.getSeverityLevel().name()
        ));
        if (targetStatus == AlertStatus.ACKNOWLEDGED) {
            activityLogService.logEvent(
                    ActivityEventType.ALERT_ACKNOWLEDGED,
                    "Alert",
                    alertId,
                    alert.getLoan().getId(),
                    "Alert acknowledged"
            );
        } else if (targetStatus == AlertStatus.RESOLVED) {
            activityLogService.logEvent(
                    ActivityEventType.ALERT_RESOLVED,
                    "Alert",
                    alertId,
                    alert.getLoan().getId(),
                    "Alert resolved"
            );
        }
        return saved;
    }

    private void validateTransition(AlertStatus currentStatus, AlertStatus targetStatus) {
        boolean allowed = switch (currentStatus) {
            case OPEN -> targetStatus == AlertStatus.ACKNOWLEDGED;
            case ACKNOWLEDGED -> targetStatus == AlertStatus.UNDER_REVIEW || targetStatus == AlertStatus.RESOLVED;
            case UNDER_REVIEW -> targetStatus == AlertStatus.RESOLVED || targetStatus == AlertStatus.OPEN;
            case RESOLVED -> false;
        };
        if (!allowed) {
            throw new ConflictException("Transition is not allowed");
        }
    }

    private void validateRole(AlertStatus currentStatus, AlertStatus targetStatus) {
        if (targetStatus == AlertStatus.ACKNOWLEDGED) {
            if (!(currentUserService.hasRole("ANALYST") || currentUserService.hasRole("ADMIN"))) {
                throw new ForbiddenOperationException("You do not have permission to perform this action");
            }
            return;
        }
        if (targetStatus == AlertStatus.UNDER_REVIEW
                || targetStatus == AlertStatus.RESOLVED
                || (currentStatus == AlertStatus.UNDER_REVIEW && targetStatus == AlertStatus.OPEN)) {
            if (!(currentUserService.hasRole("RISK_LEAD") || currentUserService.hasRole("ADMIN"))) {
                throw new ForbiddenOperationException("You do not have permission to perform this action");
            }
        }
    }
}
