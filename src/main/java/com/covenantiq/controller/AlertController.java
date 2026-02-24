package com.covenantiq.controller;

import com.covenantiq.dto.request.UpdateAlertStatusRequest;
import com.covenantiq.dto.response.AlertResponse;
import com.covenantiq.mapper.ResponseMapper;
import com.covenantiq.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PatchMapping("/{alertId}/status")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public AlertResponse updateStatus(@PathVariable Long alertId, @Valid @RequestBody UpdateAlertStatusRequest request) {
        return ResponseMapper.toAlertResponse(
                alertService.updateStatus(alertId, request.status(), request.resolutionNotes())
        );
    }
}
