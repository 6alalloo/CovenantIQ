package com.covenantiq.controller;

import com.covenantiq.dto.request.CreateChangeRequestRequest;
import com.covenantiq.dto.request.CreateReleaseRequest;
import com.covenantiq.dto.request.RollbackReleaseRequest;
import com.covenantiq.dto.response.ChangeRequestResponse;
import com.covenantiq.dto.response.ReleaseBatchResponse;
import com.covenantiq.service.ChangeControlService;
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
public class ChangeControlController {

    private final ChangeControlService changeControlService;

    public ChangeControlController(ChangeControlService changeControlService) {
        this.changeControlService = changeControlService;
    }

    @PostMapping("/change-requests")
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public ChangeRequestResponse createChangeRequest(@Valid @RequestBody CreateChangeRequestRequest request) {
        return changeControlService.createChangeRequest(request);
    }

    @PatchMapping("/change-requests/{id}/approve")
    @PreAuthorize("hasAnyRole('RISK_LEAD','ADMIN')")
    public ChangeRequestResponse approve(@PathVariable Long id) {
        return changeControlService.approveChangeRequest(id);
    }

    @GetMapping("/change-requests")
    @PreAuthorize("hasAnyRole('RISK_LEAD','ADMIN')")
    public List<ChangeRequestResponse> listChangeRequests() {
        return changeControlService.listChangeRequests();
    }

    @PostMapping("/releases")
    @PreAuthorize("hasAnyRole('RISK_LEAD','ADMIN')")
    public ReleaseBatchResponse createRelease(@Valid @RequestBody CreateReleaseRequest request) {
        return changeControlService.createRelease(request);
    }

    @PostMapping("/releases/{id}/rollback")
    @PreAuthorize("hasAnyRole('RISK_LEAD','ADMIN')")
    public ReleaseBatchResponse rollback(@PathVariable Long id, @Valid @RequestBody RollbackReleaseRequest request) {
        return changeControlService.rollbackRelease(id, request);
    }

    @GetMapping("/releases")
    @PreAuthorize("hasAnyRole('RISK_LEAD','ADMIN')")
    public List<ReleaseBatchResponse> listReleases() {
        return changeControlService.listReleases();
    }
}
