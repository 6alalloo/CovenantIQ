package com.covenantiq.service;

import com.covenantiq.domain.Covenant;
import com.covenantiq.domain.Loan;
import com.covenantiq.dto.request.CreateCovenantRequest;
import com.covenantiq.dto.request.UpdateCovenantRequest;
import com.covenantiq.enums.ActivityEventType;
import com.covenantiq.exception.ConflictException;
import com.covenantiq.exception.ResourceNotFoundException;
import com.covenantiq.repository.CovenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CovenantService {

    private final CovenantRepository covenantRepository;
    private final LoanService loanService;
    private final ActivityLogService activityLogService;
    private final OutboxEventPublisher outboxEventPublisher;

    public CovenantService(
            CovenantRepository covenantRepository,
            LoanService loanService,
            ActivityLogService activityLogService,
            OutboxEventPublisher outboxEventPublisher
    ) {
        this.covenantRepository = covenantRepository;
        this.loanService = loanService;
        this.activityLogService = activityLogService;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Transactional
    public Covenant createCovenant(Long loanId, CreateCovenantRequest request) {
        Loan loan = loanService.getLoan(loanId);
        loanService.ensureActive(loan);

        covenantRepository.findByLoanIdAndType(loanId, request.type()).ifPresent(existing -> {
            throw new ConflictException("Loan " + loanId + " already has covenant type " + request.type());
        });

        Covenant covenant = new Covenant();
        covenant.setLoan(loan);
        covenant.setType(request.type());
        covenant.setThresholdValue(request.thresholdValue());
        covenant.setComparisonType(request.comparisonType());
        covenant.setSeverityLevel(request.severityLevel());
        Covenant saved = covenantRepository.save(covenant);
        activityLogService.logEvent(
                ActivityEventType.COVENANT_CREATED,
                "Covenant",
                saved.getId(),
                loanId,
                "Covenant " + saved.getType() + " created"
        );
        outboxEventPublisher.publish("Covenant", saved.getId(), "CovenantCreated", java.util.Map.of(
                "loanId", loanId,
                "covenantId", saved.getId(),
                "type", saved.getType().name(),
                "severity", saved.getSeverityLevel().name()
        ));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Covenant> getLoanCovenants(Long loanId) {
        return covenantRepository.findByLoanIdOrderByIdAsc(loanId);
    }

    @Transactional
    public Covenant updateCovenant(Long loanId, Long covenantId, UpdateCovenantRequest request) {
        Loan loan = loanService.getLoan(loanId);
        loanService.ensureActive(loan);

        Covenant covenant = covenantRepository.findByIdAndLoanId(covenantId, loanId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Covenant " + covenantId + " not found for loan " + loanId
                ));

        covenant.setThresholdValue(request.thresholdValue());
        covenant.setComparisonType(request.comparisonType());
        covenant.setSeverityLevel(request.severityLevel());

        Covenant saved = covenantRepository.save(covenant);
        activityLogService.logEvent(
                ActivityEventType.COVENANT_UPDATED,
                "Covenant",
                saved.getId(),
                loanId,
                "Covenant " + saved.getType() + " updated"
        );
        outboxEventPublisher.publish("Covenant", saved.getId(), "CovenantUpdated", java.util.Map.of(
                "loanId", loanId,
                "covenantId", saved.getId(),
                "type", saved.getType().name(),
                "severity", saved.getSeverityLevel().name()
        ));
        return saved;
    }
}
