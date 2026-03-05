package com.covenantiq.controller;

import com.covenantiq.domain.Ruleset;
import com.covenantiq.domain.RulesetVersion;
import com.covenantiq.dto.request.CreateRulesetRequest;
import com.covenantiq.dto.request.CreateRulesetVersionRequest;
import com.covenantiq.dto.request.PublishWithReasonRequest;
import com.covenantiq.dto.request.ValidateRulesetVersionRequest;
import com.covenantiq.dto.response.RulesetResponse;
import com.covenantiq.dto.response.RulesetValidationResponse;
import com.covenantiq.dto.response.RulesetVersionResponse;
import com.covenantiq.service.RulesetService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rulesets")
public class RulesetController {

    private final RulesetService rulesetService;

    public RulesetController(RulesetService rulesetService) {
        this.rulesetService = rulesetService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public RulesetResponse createRuleset(@Valid @RequestBody CreateRulesetRequest request) {
        return toResponse(rulesetService.createRuleset(request));
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public RulesetVersionResponse createVersion(
            @PathVariable Long id,
            @Valid @RequestBody CreateRulesetVersionRequest request
    ) {
        return toResponse(rulesetService.createVersion(id, request));
    }

    @PostMapping("/{id}/versions/{version}/validate")
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public RulesetValidationResponse validateVersion(
            @PathVariable Long id,
            @PathVariable int version,
            @RequestBody(required = false) ValidateRulesetVersionRequest request
    ) {
        ValidateRulesetVersionRequest payload = request == null
                ? new ValidateRulesetVersionRequest(java.util.Map.of(), java.util.Map.of())
                : request;
        return rulesetService.validateVersion(id, version, payload);
    }

    @PostMapping("/{id}/versions/{version}/publish")
    @PreAuthorize("hasAnyRole('RISK_LEAD','ADMIN')")
    public RulesetVersionResponse publishVersion(
            @PathVariable Long id,
            @PathVariable int version,
            @Valid @RequestBody PublishWithReasonRequest request
    ) {
        return toResponse(rulesetService.publishVersion(id, version, request.reason()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public List<RulesetResponse> listRulesets() {
        return rulesetService.listRulesets().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public List<RulesetVersionResponse> listVersions(@PathVariable Long id) {
        return rulesetService.listVersions(id).stream().map(this::toResponse).toList();
    }

    private RulesetResponse toResponse(Ruleset ruleset) {
        return new RulesetResponse(
                ruleset.getId(),
                ruleset.getKey(),
                ruleset.getName(),
                ruleset.getDomain(),
                ruleset.getOwnerRole(),
                ruleset.getCreatedBy(),
                ruleset.getCreatedAt()
        );
    }

    private RulesetVersionResponse toResponse(RulesetVersion version) {
        return new RulesetVersionResponse(
                version.getId(),
                version.getRulesetId(),
                version.getVersion(),
                version.getStatus(),
                version.getDefinitionJson(),
                version.getSchemaVersion(),
                version.getChangeSummary(),
                version.getCreatedBy(),
                version.getApprovedBy(),
                version.getPublishedAt(),
                version.getCreatedAt()
        );
    }
}
