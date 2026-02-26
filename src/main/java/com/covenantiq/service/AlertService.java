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

    public AlertService(
            AlertRepository alertRepository,
            CurrentUserService currentUserService,
            ActivityLogService activityLogService
    ) {
        this.alertRepository = alertRepository;
        this.currentUserService = currentUserService;
        this.activityLogService = activityLogService;
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
        return alertRepository.save(alert);
    }

    @Transactional
    public Alert updateStatus(Long alertId, AlertStatus targetStatus, String resolutionNotes) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert " + alertId + " not found"));
        if (alert.isSuperseded()) {
            throw new ConflictException("Cannot update superseded alert");
        }
        validateTransition(alert.getStatus(), targetStatus);
        enforceRoleForTargetStatus(targetStatus);
        AlertStatus previousStatus = alert.getStatus();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String username = currentUserService.usernameOrSystem();
        switch (targetStatus) {
            case ACKNOWLEDGED -> {
                alert.setAcknowledgedBy(username);
                alert.setAcknowledgedAt(now);
            }
            case RESOLVED -> {
                if (resolutionNotes == null || resolutionNotes.isBlank()) {
                    throw new UnprocessableEntityException("resolutionNotes is required when resolving an alert");
                }
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

    private void validateTransition(AlertStatus current, AlertStatus target) {
        if (current == AlertStatus.RESOLVED && target != AlertStatus.RESOLVED) {
            throw new ConflictException("Cannot transition from RESOLVED to " + target);
        }
        if (current == target) {
            return;
        }
        if (current == AlertStatus.OPEN && (target == AlertStatus.UNDER_REVIEW || target == AlertStatus.RESOLVED)) {
            throw new ConflictException("OPEN alerts must be acknowledged before review or resolution");
        }
        if (target == AlertStatus.OPEN && current != AlertStatus.OPEN) {
            throw new ConflictException("Cannot transition back to OPEN");
        }
        if (current == AlertStatus.ACKNOWLEDGED && target == AlertStatus.OPEN) {
            throw new ConflictException("Cannot transition from ACKNOWLEDGED to OPEN");
        }
    }

    private void enforceRoleForTargetStatus(AlertStatus targetStatus) {
        if (targetStatus == AlertStatus.ACKNOWLEDGED && !(currentUserService.hasRole("ANALYST")
                || currentUserService.hasRole("ADMIN"))) {
            throw new ForbiddenOperationException("Only ANALYST or ADMIN can acknowledge alerts");
        }
        if ((targetStatus == AlertStatus.UNDER_REVIEW || targetStatus == AlertStatus.RESOLVED)
                && !(currentUserService.hasRole("RISK_LEAD") || currentUserService.hasRole("ADMIN"))) {
            throw new ForbiddenOperationException("Only RISK_LEAD or ADMIN can review/resolve alerts");
        }
    }
}
