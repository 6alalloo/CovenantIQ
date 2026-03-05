package com.covenantiq.service;

import com.covenantiq.dto.request.CreateWorkflowDefinitionRequest;
import com.covenantiq.exception.UnprocessableEntityException;
import com.covenantiq.repository.WorkflowDefinitionRepository;
import com.covenantiq.repository.WorkflowInstanceRepository;
import com.covenantiq.repository.WorkflowStateRepository;
import com.covenantiq.repository.WorkflowTransitionLogRepository;
import com.covenantiq.repository.WorkflowTransitionRepository;
import com.covenantiq.security.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock
    private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Mock
    private WorkflowStateRepository workflowStateRepository;

    @Mock
    private WorkflowTransitionRepository workflowTransitionRepository;

    @Mock
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Mock
    private WorkflowTransitionLogRepository workflowTransitionLogRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @Test
    void createDefinitionRejectsTransitionThatReferencesUndefinedStates() {
        WorkflowService service = new WorkflowService(
                workflowDefinitionRepository,
                workflowStateRepository,
                workflowTransitionRepository,
                workflowInstanceRepository,
                workflowTransitionLogRepository,
                currentUserService,
                new ObjectMapper(),
                outboxEventPublisher,
                true
        );

        CreateWorkflowDefinitionRequest request = new CreateWorkflowDefinitionRequest(
                "ALERT",
                "Invalid Workflow",
                List.of(
                        new CreateWorkflowDefinitionRequest.WorkflowStateInput("OPEN", true, false),
                        new CreateWorkflowDefinitionRequest.WorkflowStateInput("RESOLVED", false, true)
                ),
                List.of(
                        new CreateWorkflowDefinitionRequest.WorkflowTransitionInput(
                                "OPEN",
                                "UNDER_REVIEW",
                                List.of("ADMIN"),
                                List.of(),
                                null
                        )
                )
        );

        assertThrows(UnprocessableEntityException.class, () -> service.createDefinition(request));

        verify(workflowDefinitionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}


