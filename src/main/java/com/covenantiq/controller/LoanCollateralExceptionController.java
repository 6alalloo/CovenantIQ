package com.covenantiq.controller;

import com.covenantiq.domain.CollateralAsset;
import com.covenantiq.domain.CovenantException;
import com.covenantiq.dto.request.CreateCollateralAssetRequest;
import com.covenantiq.dto.request.CreateCovenantExceptionRequest;
import com.covenantiq.dto.response.CollateralAssetResponse;
import com.covenantiq.dto.response.CovenantExceptionResponse;
import com.covenantiq.service.CollateralExceptionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class LoanCollateralExceptionController {

    private final CollateralExceptionService collateralExceptionService;

    public LoanCollateralExceptionController(CollateralExceptionService collateralExceptionService) {
        this.collateralExceptionService = collateralExceptionService;
    }

    @PostMapping("/loans/{loanId}/collaterals")
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public CollateralAssetResponse createCollateral(
            @PathVariable Long loanId,
            @Valid @RequestBody CreateCollateralAssetRequest request
    ) {
        return toResponse(collateralExceptionService.createCollateral(loanId, request));
    }

    @GetMapping("/loans/{loanId}/collaterals")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public List<CollateralAssetResponse> listCollaterals(@PathVariable Long loanId) {
        return collateralExceptionService.listCollaterals(loanId).stream().map(this::toResponse).toList();
    }

    @PostMapping("/loans/{loanId}/exceptions")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public CovenantExceptionResponse requestException(
            @PathVariable Long loanId,
            @Valid @RequestBody CreateCovenantExceptionRequest request
    ) {
        return toResponse(collateralExceptionService.requestException(loanId, request));
    }

    @GetMapping("/loans/{loanId}/exceptions")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public List<CovenantExceptionResponse> listExceptions(@PathVariable Long loanId) {
        return collateralExceptionService.listExceptions(loanId).stream().map(this::toResponse).toList();
    }

    @PatchMapping("/exceptions/{id}/approve")
    @PreAuthorize("hasAnyRole('RISK_LEAD','ADMIN')")
    public CovenantExceptionResponse approve(@PathVariable Long id) {
        return toResponse(collateralExceptionService.approveException(id));
    }

    @PatchMapping("/exceptions/{id}/expire")
    @PreAuthorize("hasAnyRole('RISK_LEAD','ADMIN')")
    public CovenantExceptionResponse expire(@PathVariable Long id) {
        return toResponse(collateralExceptionService.expireException(id));
    }

    private CollateralAssetResponse toResponse(CollateralAsset asset) {
        return new CollateralAssetResponse(
                asset.getId(),
                asset.getLoanId(),
                asset.getAssetType(),
                asset.getDescription(),
                asset.getNominalValue(),
                asset.getHaircutPct(),
                asset.getNetEligibleValue(),
                asset.getLienRank(),
                asset.getCurrency(),
                asset.getEffectiveDate(),
                asset.getCreatedAt()
        );
    }

    private CovenantExceptionResponse toResponse(CovenantException exception) {
        return new CovenantExceptionResponse(
                exception.getId(),
                exception.getLoanId(),
                exception.getCovenantId(),
                exception.getExceptionType(),
                exception.getReason(),
                exception.getEffectiveFrom(),
                exception.getEffectiveTo(),
                exception.getStatus(),
                exception.getRequestedBy(),
                exception.getApprovedBy(),
                exception.getApprovedAt(),
                exception.getControlsJson(),
                exception.getCreatedAt()
        );
    }
}
