package com.covenantiq.service;

import com.covenantiq.domain.Alert;
import com.covenantiq.domain.Loan;
import com.covenantiq.enums.AlertStatus;
import com.covenantiq.exception.ConflictException;
import com.covenantiq.exception.ForbiddenOperationException;
import com.covenantiq.exception.ResourceNotFoundException;
import com.covenantiq.exception.UnprocessableEntityException;
import com.covenantiq.repository.AlertRepository;
import com.covenantiq.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    private AlertService alertService;

    @Test
    void updateStatusThrowsWhenAlertNotFound() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> alertService.updateStatus(99L, AlertStatus.ACKNOWLEDGED, null));
    }

    @Test
    void updateStatusThrowsWhenAlertIsSuperseded() {
        Alert alert = alert(AlertStatus.OPEN);
        alert.setSuperseded(true);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        assertThrows(ConflictException.class, () -> alertService.updateStatus(1L, AlertStatus.ACKNOWLEDGED, null));
    }

    @Test
    void acknowledgeOpenAlertAsAnalyst() {
        Alert alert = alert(AlertStatus.OPEN);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(currentUserService.usernameOrSystem()).thenReturn("analyst@demo.com");
        when(currentUserService.hasRole("ANALYST")).thenReturn(true);
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert saved = alertService.updateStatus(1L, AlertStatus.ACKNOWLEDGED, null);

        assertEquals(AlertStatus.ACKNOWLEDGED, saved.getStatus());
        assertEquals("analyst@demo.com", saved.getAcknowledgedBy());
        assertNotNull(saved.getAcknowledgedAt());
        verify(alertRepository).save(alert);
    }

    @Test
    void cannotTransitionOpenDirectlyToUnderReview() {
        Alert alert = alert(AlertStatus.OPEN);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        assertThrows(ConflictException.class, () -> alertService.updateStatus(1L, AlertStatus.UNDER_REVIEW, null));
        verify(currentUserService, never()).usernameOrSystem();
    }

    @Test
    void resolveRequiresResolutionNotes() {
        Alert alert = alert(AlertStatus.ACKNOWLEDGED);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        assertThrows(UnprocessableEntityException.class, () -> alertService.updateStatus(1L, AlertStatus.RESOLVED, " "));
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void resolveForbiddenForUnauthorizedRole() {
        Alert alert = alert(AlertStatus.ACKNOWLEDGED);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(currentUserService.hasRole("RISK_LEAD")).thenReturn(false);
        when(currentUserService.hasRole("ADMIN")).thenReturn(false);

        assertThrows(ForbiddenOperationException.class, () -> alertService.updateStatus(1L, AlertStatus.RESOLVED, "resolved"));
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void resolveAcknowledgedAlertWithNotesAsRiskLead() {
        Alert alert = alert(AlertStatus.ACKNOWLEDGED);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(currentUserService.usernameOrSystem()).thenReturn("risklead@demo.com");
        when(currentUserService.hasRole("RISK_LEAD")).thenReturn(true);
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert saved = alertService.updateStatus(1L, AlertStatus.RESOLVED, " done ");

        assertEquals(AlertStatus.RESOLVED, saved.getStatus());
        assertEquals("risklead@demo.com", saved.getResolvedBy());
        assertNotNull(saved.getResolvedAt());
        assertEquals("done", saved.getResolutionNotes());
    }

    @Test
    void cannotTransitionFromResolvedToOpen() {
        Alert alert = alert(AlertStatus.RESOLVED);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        assertThrows(ConflictException.class, () -> alertService.updateStatus(1L, AlertStatus.OPEN, null));
        verify(alertRepository, never()).save(any(Alert.class));
    }

    private Alert alert(AlertStatus status) {
        Alert alert = new Alert();
        alert.setId(1L);
        alert.setStatus(status);
        alert.setSuperseded(false);
        alert.setSeverityLevel(com.covenantiq.enums.SeverityLevel.HIGH);
        Loan loan = new Loan();
        loan.setId(10L);
        alert.setLoan(loan);
        return alert;
    }
}
