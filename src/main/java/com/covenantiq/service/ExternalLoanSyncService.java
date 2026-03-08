package com.covenantiq.service;

import com.covenantiq.domain.Loan;
import com.covenantiq.dto.request.LoanImportCsvRow;
import com.covenantiq.enums.ActivityEventType;
import com.covenantiq.enums.LoanImportRowAction;
import com.covenantiq.enums.LoanStatus;
import com.covenantiq.exception.UnprocessableEntityException;
import com.covenantiq.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class ExternalLoanSyncService {

    private final LoanRepository loanRepository;
    private final ActivityLogService activityLogService;
    private final OutboxEventPublisher outboxEventPublisher;

    public ExternalLoanSyncService(
            LoanRepository loanRepository,
            ActivityLogService activityLogService,
            OutboxEventPublisher outboxEventPublisher
    ) {
        this.loanRepository = loanRepository;
        this.activityLogService = activityLogService;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Transactional
    public SyncResult sync(LoanImportCsvRow row) {
        Optional<Loan> existing = loanRepository.findBySourceSystemAndExternalLoanId(row.sourceSystem(), row.externalLoanId());
        if (existing.isEmpty()) {
            Loan created = new Loan();
            applyOwnedFields(created, row);
            created.setSyncManaged(true);
            created.setLastSyncedAt(OffsetDateTime.now(ZoneOffset.UTC));
            Loan saved = loanRepository.save(created);
            activityLogService.logEvent(
                    ActivityEventType.LOAN_IMPORTED,
                    "Loan",
                    saved.getId(),
                    saved.getId(),
                    "Loan imported from " + row.sourceSystem() + " with external ID " + row.externalLoanId()
            );
            outboxEventPublisher.publish("Loan", saved.getId(), "LoanImported", java.util.Map.of(
                    "loanId", saved.getId(),
                    "sourceSystem", saved.getSourceSystem(),
                    "externalLoanId", saved.getExternalLoanId(),
                    "status", saved.getStatus().name()
            ));
            return new SyncResult(LoanImportRowAction.CREATE, saved.getId(), null);
        }

        Loan loan = existing.get();
        validateStatusTransition(loan.getStatus(), row.status(), row.externalLoanId());
        Set<String> changedFields = collectChanges(loan, row);
        if (changedFields.isEmpty()) {
            loan.setLastSyncedAt(OffsetDateTime.now(ZoneOffset.UTC));
            loanRepository.save(loan);
            return new SyncResult(LoanImportRowAction.UNCHANGED, loan.getId(), null);
        }

        applyOwnedFields(loan, row);
        loan.setLastSyncedAt(OffsetDateTime.now(ZoneOffset.UTC));
        Loan saved = loanRepository.save(loan);
        activityLogService.logEvent(
                ActivityEventType.LOAN_SYNC_UPDATED,
                "Loan",
                saved.getId(),
                saved.getId(),
                "Loan sync updated fields: " + String.join(", ", changedFields)
        );
        outboxEventPublisher.publish("Loan", saved.getId(), "LoanUpdatedFromImport", java.util.Map.of(
                "loanId", saved.getId(),
                "sourceSystem", saved.getSourceSystem(),
                "externalLoanId", saved.getExternalLoanId(),
                "changedFields", String.join(",", changedFields),
                "status", saved.getStatus().name()
        ));
        return new SyncResult(LoanImportRowAction.UPDATE, saved.getId(), null);
    }

    private void validateStatusTransition(LoanStatus currentStatus, LoanStatus incomingStatus, String externalLoanId) {
        if (currentStatus == LoanStatus.CLOSED && incomingStatus == LoanStatus.ACTIVE) {
            throw new UnprocessableEntityException(
                    "Imported loan " + externalLoanId + " cannot transition from CLOSED to ACTIVE automatically"
            );
        }
    }

    private Set<String> collectChanges(Loan loan, LoanImportCsvRow row) {
        Set<String> changed = new LinkedHashSet<>();
        if (!loan.getBorrowerName().equals(row.borrowerName())) changed.add("borrowerName");
        if (loan.getPrincipalAmount().compareTo(row.principalAmount()) != 0) changed.add("principalAmount");
        if (!loan.getStartDate().equals(row.startDate())) changed.add("startDate");
        if (loan.getStatus() != row.status()) changed.add("status");
        if (!safeEquals(loan.getSourceUpdatedAt(), row.sourceUpdatedAt())) changed.add("sourceUpdatedAt");
        return changed;
    }

    private void applyOwnedFields(Loan loan, LoanImportCsvRow row) {
        loan.setSourceSystem(row.sourceSystem());
        loan.setExternalLoanId(row.externalLoanId());
        loan.setBorrowerName(row.borrowerName());
        loan.setPrincipalAmount(row.principalAmount());
        loan.setStartDate(row.startDate());
        loan.setStatus(row.status());
        loan.setSourceUpdatedAt(row.sourceUpdatedAt());
        loan.setSyncManaged(true);
    }

    private boolean safeEquals(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    public record SyncResult(LoanImportRowAction action, Long loanId, String message) {
    }
}
