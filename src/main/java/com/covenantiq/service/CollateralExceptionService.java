package com.covenantiq.service;

import com.covenantiq.domain.CollateralAsset;
import com.covenantiq.domain.CovenantException;
import com.covenantiq.dto.request.CreateCollateralAssetRequest;
import com.covenantiq.dto.request.CreateCovenantExceptionRequest;
import com.covenantiq.enums.CovenantExceptionStatus;
import com.covenantiq.exception.ResourceNotFoundException;
import com.covenantiq.exception.UnprocessableEntityException;
import com.covenantiq.repository.CollateralAssetRepository;
import com.covenantiq.repository.CovenantExceptionRepository;
import com.covenantiq.security.CurrentUserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CollateralExceptionService {

    private final LoanService loanService;
    private final CollateralAssetRepository collateralAssetRepository;
    private final CovenantExceptionRepository covenantExceptionRepository;
    private final CurrentUserService currentUserService;
    private final OutboxEventPublisher outboxEventPublisher;

    public CollateralExceptionService(
            LoanService loanService,
            CollateralAssetRepository collateralAssetRepository,
            CovenantExceptionRepository covenantExceptionRepository,
            CurrentUserService currentUserService,
            OutboxEventPublisher outboxEventPublisher
    ) {
        this.loanService = loanService;
        this.collateralAssetRepository = collateralAssetRepository;
        this.covenantExceptionRepository = covenantExceptionRepository;
        this.currentUserService = currentUserService;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Transactional
    public CollateralAsset createCollateral(Long loanId, CreateCollateralAssetRequest request) {
        loanService.getLoan(loanId);
        CollateralAsset asset = new CollateralAsset();
        asset.setLoanId(loanId);
        asset.setAssetType(request.assetType().trim().toUpperCase());
        asset.setDescription(request.description());
        asset.setNominalValue(request.nominalValue());
        asset.setHaircutPct(request.haircutPct());
        BigDecimal eligible = request.nominalValue().multiply(BigDecimal.ONE.subtract(request.haircutPct()));
        asset.setNetEligibleValue(eligible);
        asset.setLienRank(request.lienRank());
        asset.setCurrency(request.currency().trim().toUpperCase());
        asset.setEffectiveDate(request.effectiveDate());
        return collateralAssetRepository.save(asset);
    }

    @Transactional(readOnly = true)
    public List<CollateralAsset> listCollaterals(Long loanId) {
        loanService.getLoan(loanId);
        return collateralAssetRepository.findByLoanIdOrderByIdDesc(loanId);
    }

    @Transactional
    public CovenantException requestException(Long loanId, CreateCovenantExceptionRequest request) {
        loanService.getLoan(loanId);
        if (request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new UnprocessableEntityException("effectiveTo must be >= effectiveFrom");
        }
        CovenantException exception = new CovenantException();
        exception.setLoanId(loanId);
        exception.setCovenantId(request.covenantId());
        exception.setExceptionType(request.exceptionType());
        exception.setReason(request.reason().trim());
        exception.setEffectiveFrom(request.effectiveFrom());
        exception.setEffectiveTo(request.effectiveTo());
        exception.setStatus(CovenantExceptionStatus.REQUESTED);
        exception.setRequestedBy(currentUserService.usernameOrSystem());
        exception.setControlsJson(request.controlsJson() == null || request.controlsJson().isBlank() ? "{}" : request.controlsJson());
        CovenantException saved = covenantExceptionRepository.save(exception);
        outboxEventPublisher.publish("CovenantException", saved.getId(), "ExceptionRequested", Map.of(
                "exceptionId", saved.getId(),
                "loanId", saved.getLoanId(),
                "covenantId", saved.getCovenantId()
        ));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CovenantException> listExceptions(Long loanId) {
        loanService.getLoan(loanId);
        return covenantExceptionRepository.findByLoanIdOrderByIdDesc(loanId);
    }

    @Transactional
    public CovenantException approveException(Long id) {
        CovenantException exception = covenantExceptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exception " + id + " not found"));
        exception.setStatus(CovenantExceptionStatus.APPROVED);
        exception.setApprovedBy(currentUserService.usernameOrSystem());
        exception.setApprovedAt(OffsetDateTime.now(ZoneOffset.UTC));
        CovenantException saved = covenantExceptionRepository.save(exception);
        outboxEventPublisher.publish("CovenantException", saved.getId(), "ExceptionApproved", Map.of(
                "exceptionId", saved.getId(),
                "loanId", saved.getLoanId(),
                "covenantId", saved.getCovenantId()
        ));
        return saved;
    }

    @Transactional
    public CovenantException expireException(Long id) {
        CovenantException exception = covenantExceptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exception " + id + " not found"));
        exception.setStatus(CovenantExceptionStatus.EXPIRED);
        CovenantException saved = covenantExceptionRepository.save(exception);
        outboxEventPublisher.publish("CovenantException", saved.getId(), "ExceptionExpired", Map.of(
                "exceptionId", saved.getId(),
                "loanId", saved.getLoanId(),
                "covenantId", saved.getCovenantId()
        ));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<CovenantException> getActiveApprovedException(Long loanId, Long covenantId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return covenantExceptionRepository
                .findFirstByLoanIdAndCovenantIdAndStatusAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                        loanId,
                        covenantId,
                        CovenantExceptionStatus.APPROVED,
                        today,
                        today
                );
    }

    @Transactional
    @Scheduled(cron = "0 15 0 * * *")
    public void expireElapsedExceptions() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<CovenantException> expired = covenantExceptionRepository
                .findByStatusAndEffectiveToBefore(CovenantExceptionStatus.APPROVED, today);
        for (CovenantException exception : expired) {
            exception.setStatus(CovenantExceptionStatus.EXPIRED);
            covenantExceptionRepository.save(exception);
            outboxEventPublisher.publish("CovenantException", exception.getId(), "ExceptionExpired", Map.of(
                    "exceptionId", exception.getId(),
                    "loanId", exception.getLoanId(),
                    "covenantId", exception.getCovenantId()
            ));
        }
    }
}
