package com.covenantiq.service;

import com.covenantiq.domain.Ruleset;
import com.covenantiq.domain.RulesetPublishAudit;
import com.covenantiq.domain.RulesetTestCase;
import com.covenantiq.domain.RulesetVersion;
import com.covenantiq.dto.request.CreateRulesetRequest;
import com.covenantiq.dto.request.CreateRulesetVersionRequest;
import com.covenantiq.dto.request.ValidateRulesetVersionRequest;
import com.covenantiq.dto.response.RulesetValidationResponse;
import com.covenantiq.enums.RulesetDomain;
import com.covenantiq.enums.RulesetVersionStatus;
import com.covenantiq.exception.ResourceNotFoundException;
import com.covenantiq.exception.UnprocessableEntityException;
import com.covenantiq.repository.RulesetPublishAuditRepository;
import com.covenantiq.repository.RulesetRepository;
import com.covenantiq.repository.RulesetTestCaseRepository;
import com.covenantiq.repository.RulesetVersionRepository;
import com.covenantiq.security.CurrentUserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RulesetService {

    private static final String DEFAULT_RULESET_KEY = "COVENANT_EVAL_DEFAULT";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> RULES_TYPE = new TypeReference<>() {
    };

    private final RulesetRepository rulesetRepository;
    private final RulesetVersionRepository rulesetVersionRepository;
    private final RulesetTestCaseRepository rulesetTestCaseRepository;
    private final RulesetPublishAuditRepository rulesetPublishAuditRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final OutboxEventPublisher outboxEventPublisher;

    public RulesetService(
            RulesetRepository rulesetRepository,
            RulesetVersionRepository rulesetVersionRepository,
            RulesetTestCaseRepository rulesetTestCaseRepository,
            RulesetPublishAuditRepository rulesetPublishAuditRepository,
            CurrentUserService currentUserService,
            ObjectMapper objectMapper,
            OutboxEventPublisher outboxEventPublisher
    ) {
        this.rulesetRepository = rulesetRepository;
        this.rulesetVersionRepository = rulesetVersionRepository;
        this.rulesetTestCaseRepository = rulesetTestCaseRepository;
        this.rulesetPublishAuditRepository = rulesetPublishAuditRepository;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @PostConstruct
    @Transactional
    public void seedDefaultRuleset() {
        if (rulesetRepository.findByKey(DEFAULT_RULESET_KEY).isPresent()) {
            return;
        }
        Ruleset ruleset = new Ruleset();
        ruleset.setKey(DEFAULT_RULESET_KEY);
        ruleset.setName("Default Covenant Evaluation Rules");
        ruleset.setDomain(RulesetDomain.COVENANT_EVAL);
        ruleset.setOwnerRole("RISK_LEAD");
        ruleset.setCreatedBy("system");
        Ruleset savedRuleset = rulesetRepository.save(ruleset);

        String defaultJson = """
                {
                  "rules": [
                    {
                      "when": {"comparisonPass": false, "activeException": true},
                      "outcome": {"breach": false, "alertType": "EARLY_WARNING", "reasonCode": "EXCEPTION_DOWNGRADED"}
                    },
                    {
                      "when": {"comparisonPass": false},
                      "outcome": {"breach": true, "alertType": "BREACH", "reasonCode": "THRESHOLD_BREACH"}
                    },
                    {
                      "when": {"comparisonPass": true},
                      "outcome": {"breach": false, "reasonCode": "PASS"}
                    }
                  ]
                }
                """;
        RulesetVersion version = new RulesetVersion();
        version.setRulesetId(savedRuleset.getId());
        version.setVersion(1);
        version.setStatus(RulesetVersionStatus.PUBLISHED);
        version.setDefinitionJson(defaultJson);
        version.setSchemaVersion(1);
        version.setChangeSummary("Bootstrap default rules");
        version.setCreatedBy("system");
        version.setApprovedBy("system");
        version.setPublishedAt(OffsetDateTime.now(ZoneOffset.UTC));
        rulesetVersionRepository.save(version);
    }

    @Transactional
    public Ruleset createRuleset(CreateRulesetRequest request) {
        Ruleset ruleset = new Ruleset();
        ruleset.setKey(request.key().trim().toUpperCase());
        ruleset.setName(request.name().trim());
        ruleset.setDomain(request.domain());
        ruleset.setOwnerRole(request.ownerRole().trim().toUpperCase());
        ruleset.setCreatedBy(currentUserService.usernameOrSystem());
        return rulesetRepository.save(ruleset);
    }

    @Transactional
    public RulesetVersion createVersion(Long rulesetId, CreateRulesetVersionRequest request) {
        Ruleset ruleset = rulesetRepository.findById(rulesetId)
                .orElseThrow(() -> new ResourceNotFoundException("Ruleset " + rulesetId + " not found"));
        validateDefinition(request.definitionJson());
        int nextVersion = rulesetVersionRepository.findByRulesetIdOrderByVersionDesc(rulesetId).stream()
                .map(RulesetVersion::getVersion)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        RulesetVersion version = new RulesetVersion();
        version.setRulesetId(ruleset.getId());
        version.setVersion(nextVersion);
        version.setStatus(RulesetVersionStatus.DRAFT);
        version.setDefinitionJson(request.definitionJson());
        version.setSchemaVersion(request.schemaVersion());
        version.setChangeSummary(request.changeSummary());
        version.setCreatedBy(currentUserService.usernameOrSystem());
        return rulesetVersionRepository.save(version);
    }

    @Transactional
    public RulesetValidationResponse validateVersion(Long rulesetId, int version, ValidateRulesetVersionRequest request) {
        RulesetVersion rulesetVersion = getVersion(rulesetId, version);
        validateDefinition(rulesetVersion.getDefinitionJson());
        Map<String, Object> actual = evaluateInternal(rulesetVersion.getDefinitionJson(), request.input() == null ? Map.of() : request.input());
        boolean pass = request.expectedOutput() == null || request.expectedOutput().isEmpty() || expectedMatches(actual, request.expectedOutput());

        RulesetTestCase testCase = new RulesetTestCase();
        testCase.setRulesetVersionId(rulesetVersion.getId());
        testCase.setInputJson(writeJson(request.input() == null ? Map.of() : request.input()));
        testCase.setExpectedOutputJson(writeJson(request.expectedOutput() == null ? Map.of() : request.expectedOutput()));
        testCase.setActualOutputJson(writeJson(actual));
        testCase.setPass(pass);
        testCase.setExecutedAt(OffsetDateTime.now(ZoneOffset.UTC));
        rulesetTestCaseRepository.save(testCase);

        rulesetVersion.setStatus(RulesetVersionStatus.VALIDATED);
        rulesetVersionRepository.save(rulesetVersion);
        return new RulesetValidationResponse(rulesetVersion.getId(), true, pass, actual, pass ? "Validation passed" : "Output mismatch");
    }

    @Transactional
    public RulesetVersion publishVersion(Long rulesetId, int version, String reason) {
        Ruleset ruleset = rulesetRepository.findById(rulesetId)
                .orElseThrow(() -> new ResourceNotFoundException("Ruleset " + rulesetId + " not found"));
        RulesetVersion target = getVersion(rulesetId, version);
        validateDefinition(target.getDefinitionJson());

        int fromVersion = rulesetVersionRepository.findByRulesetIdAndStatus(rulesetId, RulesetVersionStatus.PUBLISHED)
                .map(existing -> {
                    existing.setStatus(RulesetVersionStatus.ARCHIVED);
                    rulesetVersionRepository.save(existing);
                    return existing.getVersion();
                })
                .orElse(0);

        target.setStatus(RulesetVersionStatus.PUBLISHED);
        target.setApprovedBy(currentUserService.usernameOrSystem());
        target.setPublishedAt(OffsetDateTime.now(ZoneOffset.UTC));
        RulesetVersion saved = rulesetVersionRepository.save(target);

        RulesetPublishAudit audit = new RulesetPublishAudit();
        audit.setRulesetId(rulesetId);
        audit.setFromVersion(fromVersion);
        audit.setToVersion(saved.getVersion());
        audit.setActor(currentUserService.usernameOrSystem());
        audit.setReason(reason);
        rulesetPublishAuditRepository.save(audit);

        outboxEventPublisher.publish("RulesetVersion", saved.getId(), "RulesetVersionPublished", Map.of(
                "rulesetId", ruleset.getId(),
                "rulesetKey", ruleset.getKey(),
                "version", saved.getVersion(),
                "reason", reason
        ));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Ruleset> listRulesets() {
        return rulesetRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<RulesetVersion> listVersions(Long rulesetId) {
        return rulesetVersionRepository.findByRulesetIdOrderByVersionDesc(rulesetId);
    }

    @Transactional(readOnly = true)
    public RuleDecision evaluatePublished(String rulesetKey, Map<String, Object> context) {
        Ruleset ruleset = rulesetRepository.findByKey(rulesetKey)
                .orElseThrow(() -> new ResourceNotFoundException("Ruleset key " + rulesetKey + " not found"));
        RulesetVersion published = rulesetVersionRepository.findByRulesetIdAndStatus(ruleset.getId(), RulesetVersionStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("No published ruleset version for key " + rulesetKey));
        Map<String, Object> result = evaluateInternal(published.getDefinitionJson(), context);
        boolean breach = Boolean.TRUE.equals(result.getOrDefault("breach", false));
        String alertType = String.valueOf(result.getOrDefault("alertType", "BREACH"));
        String reasonCode = String.valueOf(result.getOrDefault("reasonCode", "UNSPECIFIED"));
        return new RuleDecision(breach, alertType, reasonCode, result);
    }

    private RulesetVersion getVersion(Long rulesetId, int version) {
        return rulesetVersionRepository.findByRulesetIdAndVersion(rulesetId, version)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ruleset version " + version + " not found for ruleset " + rulesetId
                ));
    }

    private Map<String, Object> evaluateInternal(String definitionJson, Map<String, Object> context) {
        try {
            Map<String, Object> definition = objectMapper.readValue(definitionJson, MAP_TYPE);
            Object rulesObj = definition.get("rules");
            if (!(rulesObj instanceof List<?> rules)) {
                throw new UnprocessableEntityException("rules must be an array");
            }
            for (Object ruleObj : rules) {
                if (!(ruleObj instanceof Map<?, ?> map)) {
                    continue;
                }
                Map<String, Object> rule = (Map<String, Object>) map;
                Map<String, Object> when = (Map<String, Object>) rule.getOrDefault("when", Map.of());
                if (matches(when, context)) {
                    return (Map<String, Object>) rule.getOrDefault("outcome", Map.of());
                }
            }
            return Map.of();
        } catch (UnprocessableEntityException e) {
            throw e;
        } catch (Exception ex) {
            throw new UnprocessableEntityException("Invalid rules definition JSON");
        }
    }

    private boolean matches(Map<String, Object> when, Map<String, Object> context) {
        for (Map.Entry<String, Object> entry : when.entrySet()) {
            Object value = context.get(entry.getKey());
            if (!Objects.equals(String.valueOf(entry.getValue()), String.valueOf(value))) {
                return false;
            }
        }
        return true;
    }

    private boolean expectedMatches(Map<String, Object> actual, Map<String, Object> expected) {
        for (Map.Entry<String, Object> entry : expected.entrySet()) {
            if (!Objects.equals(String.valueOf(entry.getValue()), String.valueOf(actual.get(entry.getKey())))) {
                return false;
            }
        }
        return true;
    }

    private void validateDefinition(String definitionJson) {
        try {
            Map<String, Object> root = objectMapper.readValue(definitionJson, MAP_TYPE);
            if (!root.containsKey("rules")) {
                throw new UnprocessableEntityException("definitionJson must include rules array");
            }
            objectMapper.convertValue(root.get("rules"), RULES_TYPE);
        } catch (UnprocessableEntityException e) {
            throw e;
        } catch (Exception ex) {
            throw new UnprocessableEntityException("Malformed rules definition JSON");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize JSON");
        }
    }

    public record RuleDecision(
            boolean breach,
            String alertType,
            String reasonCode,
            Map<String, Object> raw
    ) {
    }
}
