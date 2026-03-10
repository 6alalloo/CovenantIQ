package com.covenantiq.service;

import com.covenantiq.domain.Alert;
import com.covenantiq.domain.Covenant;
import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.domain.Loan;
import com.covenantiq.dto.request.LoanImportCsvRow;
import com.covenantiq.enums.ActivityEventType;
import com.covenantiq.enums.LoanImportRowAction;
import com.covenantiq.enums.LoanStatus;
import com.covenantiq.exception.UnprocessableEntityException;
import com.covenantiq.repository.LoanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalLoanSyncServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    private ExternalLoanSyncService externalLoanSyncService;

    @Test
    void createsLoanWhenNoExistingMatchIsFound() {
        LoanImportCsvRow row = importRow("CORE_BANKING", "LN-100001", "Imported Borrower", LoanStatus.ACTIVE);
        when(loanRepository.findBySourceSystemAndExternalLoanId("CORE_BANKING", "LN-100001"))
                .thenReturn(Optional.empty());
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        ExternalLoanSyncService.SyncResult result = externalLoanSyncService.sync(row);

        assertEquals(LoanImportRowAction.CREATE, result.action());
        assertEquals(42L, result.loanId());

        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(loanCaptor.capture());
        Loan saved = loanCaptor.getValue();
        assertEquals("CORE_BANKING", saved.getSourceSystem());
        assertEquals("LN-100001", saved.getExternalLoanId());
        assertEquals("Imported Borrower", saved.getBorrowerName());
        assertEquals(new BigDecimal("1250000.00"), saved.getPrincipalAmount());
        assertEquals(LocalDate.parse("2025-01-15"), saved.getStartDate());
        assertEquals(LoanStatus.ACTIVE, saved.getStatus());
        assertTrue(saved.isSyncManaged());
        assertNotNull(saved.getLastSyncedAt());
        assertEquals(OffsetDateTime.parse("2026-03-08T10:15:00Z"), saved.getSourceUpdatedAt());

        verify(activityLogService).logEvent(
                eq(ActivityEventType.LOAN_IMPORTED),
                eq("Loan"),
                eq(42L),
                eq(42L),
                eq("Loan imported from CORE_BANKING with external ID LN-100001")
        );
        verify(outboxEventPublisher).publish(eq("Loan"), eq(42L), eq("LoanImported"), anyMap());
    }

    @Test
    void updatesExistingLoanWhenMatchingSourceSystemAndExternalLoanIdExists() {
        Loan existing = existingLoan(7L, "Original Borrower", LoanStatus.ACTIVE);
        existing.setSourceUpdatedAt(OffsetDateTime.parse("2026-03-01T00:00:00Z"));
        LoanImportCsvRow row = importRow("CORE_BANKING", "LN-100001", "Updated Borrower", LoanStatus.ACTIVE);

        when(loanRepository.findBySourceSystemAndExternalLoanId("CORE_BANKING", "LN-100001"))
                .thenReturn(Optional.of(existing));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExternalLoanSyncService.SyncResult result = externalLoanSyncService.sync(row);

        assertEquals(LoanImportRowAction.UPDATE, result.action());
        assertEquals(7L, result.loanId());
        assertEquals("Updated Borrower", existing.getBorrowerName());
        assertEquals(new BigDecimal("1250000.00"), existing.getPrincipalAmount());
        assertEquals(OffsetDateTime.parse("2026-03-08T10:15:00Z"), existing.getSourceUpdatedAt());
        assertNotNull(existing.getLastSyncedAt());

        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        verify(activityLogService).logEvent(
                eq(ActivityEventType.LOAN_SYNC_UPDATED),
                eq("Loan"),
                eq(7L),
                eq(7L),
                descriptionCaptor.capture()
        );
        String description = descriptionCaptor.getValue();
        assertTrue(description.contains("borrowerName"));
        assertTrue(description.contains("principalAmount"));
        assertTrue(description.contains("sourceUpdatedAt"));
        verify(outboxEventPublisher).publish(eq("Loan"), eq(7L), eq("LoanUpdatedFromImport"), anyMap());
    }

    @Test
    void preservesUserManagedFieldsWhenUpdatingImportedLoan() {
        Loan existing = existingLoan(9L, "Original Borrower", LoanStatus.ACTIVE);
        Covenant covenant = new Covenant();
        Alert alert = new Alert();
        FinancialStatement statement = new FinancialStatement();
        existing.setCovenants(new ArrayList<>());
        existing.setAlerts(new ArrayList<>());
        existing.setFinancialStatements(new ArrayList<>());
        existing.getCovenants().add(covenant);
        existing.getAlerts().add(alert);
        existing.getFinancialStatements().add(statement);

        LoanImportCsvRow row = importRow("CORE_BANKING", "LN-100001", "Updated Borrower", LoanStatus.ACTIVE);

        when(loanRepository.findBySourceSystemAndExternalLoanId("CORE_BANKING", "LN-100001"))
                .thenReturn(Optional.of(existing));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        externalLoanSyncService.sync(row);

        assertEquals(1, existing.getCovenants().size());
        assertEquals(1, existing.getAlerts().size());
        assertEquals(1, existing.getFinancialStatements().size());
        assertSame(covenant, existing.getCovenants().get(0));
        assertSame(alert, existing.getAlerts().get(0));
        assertSame(statement, existing.getFinancialStatements().get(0));
    }

    @Test
    void closesActiveLoanWhenImportedStatusIsClosed() {
        Loan existing = existingLoan(11L, "Closing Borrower", LoanStatus.ACTIVE);
        LoanImportCsvRow row = importRow("CORE_BANKING", "LN-100001", "Closing Borrower", LoanStatus.CLOSED);

        when(loanRepository.findBySourceSystemAndExternalLoanId("CORE_BANKING", "LN-100001"))
                .thenReturn(Optional.of(existing));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExternalLoanSyncService.SyncResult result = externalLoanSyncService.sync(row);

        assertEquals(LoanImportRowAction.UPDATE, result.action());
        assertEquals(LoanStatus.CLOSED, existing.getStatus());
    }

    @Test
    void returnsUnchangedWhenImportedRowHasNoEffectiveFieldChanges() {
        Loan existing = existingLoan(15L, "Imported Borrower", LoanStatus.ACTIVE);
        existing.setPrincipalAmount(new BigDecimal("1250000.00"));
        existing.setSyncManaged(true);
        existing.setSourceUpdatedAt(OffsetDateTime.parse("2026-03-08T10:15:00Z"));
        LoanImportCsvRow row = importRow("CORE_BANKING", "LN-100001", "Imported Borrower", LoanStatus.ACTIVE);

        when(loanRepository.findBySourceSystemAndExternalLoanId("CORE_BANKING", "LN-100001"))
                .thenReturn(Optional.of(existing));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExternalLoanSyncService.SyncResult result = externalLoanSyncService.sync(row);

        assertEquals(LoanImportRowAction.UNCHANGED, result.action());
        assertEquals(15L, result.loanId());
        assertNotNull(existing.getLastSyncedAt());
        verify(loanRepository).save(existing);
        verifyNoInteractions(activityLogService, outboxEventPublisher);
    }

    @Test
    void rejectsClosedToActiveReopenWhenReopenIsNotAllowed() {
        Loan existing = existingLoan(13L, "Closed Borrower", LoanStatus.CLOSED);
        LoanImportCsvRow row = importRow("CORE_BANKING", "LN-100001", "Closed Borrower", LoanStatus.ACTIVE);

        when(loanRepository.findBySourceSystemAndExternalLoanId("CORE_BANKING", "LN-100001"))
                .thenReturn(Optional.of(existing));

        UnprocessableEntityException ex = assertThrows(
                UnprocessableEntityException.class,
                () -> externalLoanSyncService.sync(row)
        );

        assertEquals("Imported loan LN-100001 cannot transition from CLOSED to ACTIVE automatically", ex.getMessage());
        verify(loanRepository, never()).save(any(Loan.class));
        verifyNoInteractions(activityLogService, outboxEventPublisher);
    }

    private Loan existingLoan(Long id, String borrowerName, LoanStatus status) {
        Loan loan = new Loan();
        loan.setId(id);
        loan.setSourceSystem("CORE_BANKING");
        loan.setExternalLoanId("LN-100001");
        loan.setBorrowerName(borrowerName);
        loan.setPrincipalAmount(new BigDecimal("1000000.00"));
        loan.setStartDate(LocalDate.parse("2025-01-15"));
        loan.setStatus(status);
        loan.setSyncManaged(false);
        return loan;
    }

    private LoanImportCsvRow importRow(String sourceSystem, String externalLoanId, String borrowerName, LoanStatus status) {
        return new LoanImportCsvRow(
                2,
                sourceSystem,
                externalLoanId,
                borrowerName,
                new BigDecimal("1250000.00"),
                LocalDate.parse("2025-01-15"),
                status,
                OffsetDateTime.parse("2026-03-08T10:15:00Z")
        );
    }
}
