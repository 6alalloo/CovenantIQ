package com.covenantiq.service;

import com.covenantiq.domain.Alert;
import com.covenantiq.domain.WorkflowDefinition;
import com.covenantiq.domain.WorkflowInstance;
import com.covenantiq.domain.WorkflowState;
import com.covenantiq.domain.WorkflowTransition;
import com.covenantiq.domain.WorkflowTransitionLog;
import com.covenantiq.dto.request.CreateWorkflowDefinitionRequest;
import com.covenantiq.dto.response.WorkflowDefinitionResponse;
import com.covenantiq.dto.response.WorkflowInstanceResponse;
import com.covenantiq.dto.response.WorkflowStateResponse;
import com.covenantiq.dto.response.WorkflowTransitionLogResponse;
import com.covenantiq.dto.response.WorkflowTransitionResponse;
import com.covenantiq.enums.WorkflowDefinitionStatus;
import com.covenantiq.exception.ResourceNotFoundException;
import com.covenantiq.exception.UnprocessableEntityException;
import com.covenantiq.exception.WorkflowTransitionConflictException;
import com.covenantiq.repository.WorkflowDefinitionRepository;
import com.covenantiq.repository.WorkflowInstanceRepository;
import com.covenantiq.repository.WorkflowStateRepository;
import com.covenantiq.repository.WorkflowTransitionLogRepository;
import com.covenantiq.repository.WorkflowTransitionRepository;
import com.covenantiq.security.CurrentUserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class WorkflowService {

    private static final String ALERT_ENTITY = "ALERT";
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStateRepository workflowStateRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowTransitionLogRepository workflowTransitionLogRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final OutboxEventPublisher outboxEventPublisher;

    public WorkflowService(
            WorkflowDefinitionRepository workflowDefinitionRepository,
            WorkflowStateRepository workflowStateRepository,
            WorkflowTransitionRepository workflowTransitionRepository,
            WorkflowInstanceRepository workflowInstanceRepository,
            WorkflowTransitionLogRepository workflowTransitionLogRepository,
            CurrentUserService currentUserService,
            ObjectMapper objectMapper,
            OutboxEventPublisher outboxEventPublisher
    ) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowStateRepository = workflowStateRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.workflowTransitionLogRepository = workflowTransitionLogRepository;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @PostConstruct
    @Transactional
    public void ensureDefaultAlertWorkflow() {
        if (workflowDefinitionRepository.findByEntityTypeAndStatus(ALERT_ENTITY, WorkflowDefinitionStatus.PUBLISHED).isPresent()) {
            return;
        }
        CreateWorkflowDefinitionRequest request = new CreateWorkflowDefinitionRequest(
                ALERT_ENTITY,
                "Default Alert Workflow",
                List.of(
                        new CreateWorkflowDefinitionRequest.WorkflowStateInput("OPEN", true, false),
                        new CreateWorkflowDefinitionRequest.WorkflowStateInput("ACKNOWLEDGED", false, false),
                        new CreateWorkflowDefinitionRequest.WorkflowStateInput("UNDER_REVIEW", false, false),
                        new CreateWorkflowDefinitionRequest.WorkflowStateInput("RESOLVED", false, true)
                ),
                List.of(
                        new CreateWorkflowDefinitionRequest.WorkflowTransitionInput(
                                "OPEN", "ACKNOWLEDGED", List.of("ANALYST", "ADMIN"), List.of(), null
                        ),
                        new CreateWorkflowDefinitionRequest.WorkflowTransitionInput(
                                "ACKNOWLEDGED", "UNDER_REVIEW", List.of("RISK_LEAD", "ADMIN"), List.of(), null
                        ),
                        new CreateWorkflowDefinitionRequest.WorkflowTransitionInput(
                                "ACKNOWLEDGED", "RESOLVED", List.of("RISK_LEAD", "ADMIN"), List.of("resolutionNotes"), null
                        ),
                        new CreateWorkflowDefinitionRequest.WorkflowTransitionInput(
                                "UNDER_REVIEW", "RESOLVED", List.of("RISK_LEAD", "ADMIN"), List.of("resolutionNotes"), null
                        )
                )
        );
        WorkflowDefinition draft = createDefinition(request);
        publish(draft.getId(), "Bootstrapped default alert workflow");
    }

    @Transactional
    public WorkflowDefinition createDefinition(CreateWorkflowDefinitionRequest request) {
        long initialCount = request.states().stream().filter(CreateWorkflowDefinitionRequest.WorkflowStateInput::initial).count();
        if (initialCount != 1) {
            throw new UnprocessableEntityException("Exactly one initial state is required");
        }
        Set<String> validStateCodes = request.states().stream()
                .map(stateInput -> normalizeStateCode(stateInput.stateCode()))
                .collect(java.util.stream.Collectors.toSet());
        if (validStateCodes.size() != request.states().size()) {
            throw new UnprocessableEntityException("Workflow state codes must be unique");
        }
        List<String> invalidTransitionReferences = request.transitions().stream()
                .flatMap(transitionInput -> java.util.stream.Stream.of(
                        validStateCodes.contains(normalizeStateCode(transitionInput.fromState()))
                                ? null
                                : "fromState=" + normalizeStateCode(transitionInput.fromState()),
                        validStateCodes.contains(normalizeStateCode(transitionInput.toState()))
                                ? null
                                : "toState=" + normalizeStateCode(transitionInput.toState())
                ))
                .filter(java.util.Objects::nonNull)
                .toList();
        if (!invalidTransitionReferences.isEmpty()) {
            throw new UnprocessableEntityException(
                    "Transitions reference undefined workflow states: "
                            + String.join(", ", invalidTransitionReferences)
            );
        }
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setEntityType(request.entityType().trim().toUpperCase());
        definition.setName(request.name().trim());
        definition.setVersion(workflowDefinitionRepository.countByEntityType(definition.getEntityType()) + 1);
        definition.setStatus(WorkflowDefinitionStatus.DRAFT);
        definition.setCreatedBy(currentUserService.usernameOrSystem());
        WorkflowDefinition saved = workflowDefinitionRepository.save(definition);

        for (CreateWorkflowDefinitionRequest.WorkflowStateInput stateInput : request.states()) {
            WorkflowState state = new WorkflowState();
            state.setWorkflowDefinitionId(saved.getId());
            state.setStateCode(normalizeStateCode(stateInput.stateCode()));
            state.setInitialState(stateInput.initial());
            state.setTerminalState(stateInput.terminal());
            workflowStateRepository.save(state);
        }

        for (CreateWorkflowDefinitionRequest.WorkflowTransitionInput transitionInput : request.transitions()) {
            WorkflowTransition transition = new WorkflowTransition();
            transition.setWorkflowDefinitionId(saved.getId());
            transition.setFromState(normalizeStateCode(transitionInput.fromState()));
            transition.setToState(normalizeStateCode(transitionInput.toState()));
            transition.setAllowedRolesCsv(String.join(",", transitionInput.allowedRoles()).toUpperCase());
            transition.setRequiredFieldsJson(writeJson(transitionInput.requiredFields() == null ? List.of() : transitionInput.requiredFields()));
            transition.setGuardExpression(transitionInput.guardExpression());
            workflowTransitionRepository.save(transition);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<WorkflowDefinitionResponse> listDefinitions(String entityType) {
        List<WorkflowDefinition> definitions = entityType == null || entityType.isBlank()
                ? workflowDefinitionRepository.findAll()
                : workflowDefinitionRepository.findByEntityTypeOrderByVersionDesc(entityType.trim().toUpperCase());
        return definitions.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public WorkflowDefinitionResponse getDefinitionResponse(Long definitionId) {
        WorkflowDefinition definition = workflowDefinitionRepository.findById(definitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow definition " + definitionId + " not found"));
        return toResponse(definition);
    }

    @Transactional
    public WorkflowDefinition publish(Long definitionId, String reason) {
        WorkflowDefinition definition = workflowDefinitionRepository.findById(definitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow definition " + definitionId + " not found"));
        if (definition.getStatus() == WorkflowDefinitionStatus.PUBLISHED) {
            return definition;
        }
        workflowDefinitionRepository.findByEntityTypeAndStatus(definition.getEntityType(), WorkflowDefinitionStatus.PUBLISHED)
                .ifPresent(current -> {
                    current.setStatus(WorkflowDefinitionStatus.RETIRED);
                    workflowDefinitionRepository.save(current);
                });
        definition.setStatus(WorkflowDefinitionStatus.PUBLISHED);
        WorkflowDefinition saved = workflowDefinitionRepository.save(definition);
        outboxEventPublisher.publish("WorkflowDefinition", saved.getId(), "WorkflowDefinitionPublished", Map.of(
                "definitionId", saved.getId(),
                "entityType", saved.getEntityType(),
                "version", saved.getVersion(),
                "reason", reason
        ));
        return saved;
    }

    @Transactional
    public WorkflowInstance ensureInstanceForAlert(Alert alert) {
        return workflowInstanceRepository.findByEntityTypeAndEntityId(ALERT_ENTITY, alert.getId())
                .orElseGet(() -> {
                    WorkflowDefinition definition = getPublishedDefinition(ALERT_ENTITY);
                    WorkflowState initialState = workflowStateRepository
                            .findByWorkflowDefinitionIdAndInitialStateTrue(definition.getId())
                            .orElseThrow(() -> new UnprocessableEntityException("Workflow has no initial state"));
                    WorkflowInstance instance = new WorkflowInstance();
                    instance.setEntityType(ALERT_ENTITY);
                    instance.setEntityId(alert.getId());
                    instance.setWorkflowDefinitionId(definition.getId());
                    instance.setCurrentState(initialState.getStateCode());
                    return workflowInstanceRepository.save(instance);
                });
    }

    @Transactional
    public WorkflowInstance transition(
            WorkflowInstance instance,
            String toState,
            String reason,
            Map<String, Object> metadata,
            Map<String, Object> requiredFieldValues
    ) {
        String targetState = toState.trim().toUpperCase();
        WorkflowTransition transition = workflowTransitionRepository
                .findByWorkflowDefinitionIdAndFromStateAndToState(
                        instance.getWorkflowDefinitionId(),
                        instance.getCurrentState(),
                        targetState
                )
                .orElseThrow(() -> new WorkflowTransitionConflictException(
                        "Transition is not allowed",
                        Map.of("fromState", instance.getCurrentState(), "toState", targetState)
                ));

        List<String> allowedRoles = csvToList(transition.getAllowedRolesCsv());
        boolean roleAllowed = allowedRoles.stream().anyMatch(currentUserService::hasRole);
        if (!roleAllowed) {
            throw new WorkflowTransitionConflictException("Actor role is not allowed for this transition", Map.of(
                    "requiredRoles", allowedRoles,
                    "fromState", instance.getCurrentState(),
                    "toState", targetState
            ));
        }

        List<String> requiredFields = readStringList(transition.getRequiredFieldsJson());
        for (String field : requiredFields) {
            Object value = requiredFieldValues.get(field);
            if (value == null || String.valueOf(value).isBlank()) {
                throw new WorkflowTransitionConflictException("Missing required transition field", Map.of(
                        "missingField", field,
                        "fromState", instance.getCurrentState(),
                        "toState", targetState
                ));
            }
        }

        WorkflowTransitionLog log = new WorkflowTransitionLog();
        log.setWorkflowInstanceId(instance.getId());
        log.setFromState(instance.getCurrentState());
        log.setToState(targetState);
        log.setActor(currentUserService.usernameOrSystem());
        log.setReason(reason);
        log.setMetadataJson(writeJson(metadata == null ? Map.of() : metadata));
        workflowTransitionLogRepository.save(log);

        instance.setCurrentState(targetState);
        return workflowInstanceRepository.save(instance);
    }

    @Transactional(readOnly = true)
    public WorkflowInstanceResponse getInstance(String entityType, Long entityId) {
        WorkflowInstance instance = workflowInstanceRepository.findByEntityTypeAndEntityId(entityType.toUpperCase(), entityId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow instance not found"));
        return toInstanceResponse(instance);
    }

    @Transactional
    public WorkflowInstanceResponse transitionById(Long instanceId, String toState, String reason, Map<String, Object> metadata) {
        WorkflowInstance instance = workflowInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow instance " + instanceId + " not found"));
        WorkflowInstance updated = transition(
                instance,
                toState,
                reason,
                metadata == null ? Map.of() : metadata,
                metadata == null ? Map.of() : metadata
        );
        return toInstanceResponse(updated);
    }

    private WorkflowDefinition getPublishedDefinition(String entityType) {
        return workflowDefinitionRepository.findByEntityTypeAndStatus(entityType, WorkflowDefinitionStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("No published workflow definition for " + entityType));
    }

    private WorkflowDefinitionResponse toResponse(WorkflowDefinition definition) {
        List<WorkflowStateResponse> states = workflowStateRepository.findByWorkflowDefinitionIdOrderByIdAsc(definition.getId())
                .stream()
                .map(s -> new WorkflowStateResponse(s.getStateCode(), s.isInitialState(), s.isTerminalState()))
                .toList();
        List<WorkflowTransitionResponse> transitions =
                workflowTransitionRepository.findByWorkflowDefinitionIdOrderByIdAsc(definition.getId())
                        .stream()
                        .map(t -> new WorkflowTransitionResponse(
                                t.getFromState(),
                                t.getToState(),
                                csvToList(t.getAllowedRolesCsv()),
                                readStringList(t.getRequiredFieldsJson()),
                                t.getGuardExpression()
                        ))
                        .toList();
        return new WorkflowDefinitionResponse(
                definition.getId(),
                definition.getEntityType(),
                definition.getName(),
                definition.getVersion(),
                definition.getStatus(),
                definition.getCreatedBy(),
                definition.getCreatedAt(),
                states,
                transitions
        );
    }

    private WorkflowInstanceResponse toInstanceResponse(WorkflowInstance instance) {
        List<WorkflowTransitionLogResponse> logs = workflowTransitionLogRepository
                .findByWorkflowInstanceIdOrderByTimestampUtcDesc(instance.getId())
                .stream()
                .map(log -> new WorkflowTransitionLogResponse(
                        log.getId(),
                        log.getFromState(),
                        log.getToState(),
                        log.getActor(),
                        log.getReason(),
                        log.getMetadataJson(),
                        log.getTimestampUtc()
                ))
                .toList();
        return new WorkflowInstanceResponse(
                instance.getId(),
                instance.getEntityType(),
                instance.getEntityId(),
                instance.getWorkflowDefinitionId(),
                instance.getCurrentState(),
                instance.getStartedAt(),
                instance.getUpdatedAt(),
                logs
        );
    }

    private List<String> csvToList(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(csv.split(",")).map(String::trim).filter(v -> !v.isBlank()).toList();
    }

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String normalizeStateCode(String stateCode) {
        return stateCode.trim().toUpperCase();
    }

    private String writeJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize workflow payload");
        }
    }
}
