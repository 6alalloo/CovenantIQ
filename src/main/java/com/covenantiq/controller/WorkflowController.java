package com.covenantiq.controller;

import com.covenantiq.dto.request.CreateWorkflowDefinitionRequest;
import com.covenantiq.dto.request.PublishWithReasonRequest;
import com.covenantiq.dto.request.WorkflowInstanceTransitionRequest;
import com.covenantiq.dto.response.WorkflowDefinitionResponse;
import com.covenantiq.dto.response.WorkflowInstanceResponse;
import com.covenantiq.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/definitions")
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public WorkflowDefinitionResponse createDefinition(@Valid @RequestBody CreateWorkflowDefinitionRequest request) {
        return workflowService.getDefinitionResponse(workflowService.createDefinition(request).getId());
    }

    @GetMapping("/definitions")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public List<WorkflowDefinitionResponse> listDefinitions(@RequestParam(required = false) String entityType) {
        return workflowService.listDefinitions(entityType);
    }

    @PostMapping("/definitions/{id}/publish")
    @PreAuthorize("hasAnyRole('RISK_LEAD','ADMIN')")
    public WorkflowDefinitionResponse publish(
            @PathVariable Long id,
            @Valid @RequestBody PublishWithReasonRequest request
    ) {
        workflowService.publish(id, request.reason());
        return workflowService.getDefinitionResponse(id);
    }

    @GetMapping("/instances/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public WorkflowInstanceResponse getInstance(@PathVariable String entityType, @PathVariable Long entityId) {
        return workflowService.getInstance(entityType, entityId);
    }

    @PostMapping("/instances/{id}/transition")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public WorkflowInstanceResponse transition(
            @PathVariable Long id,
            @Valid @RequestBody WorkflowInstanceTransitionRequest request
    ) {
        return workflowService.transitionById(
                id,
                request.toState(),
                request.reason(),
                request.metadata() == null ? Map.of() : request.metadata()
        );
    }
}
