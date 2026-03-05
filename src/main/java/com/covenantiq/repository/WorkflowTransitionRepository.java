package com.covenantiq.repository;

import com.covenantiq.domain.WorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {

    List<WorkflowTransition> findByWorkflowDefinitionIdOrderByIdAsc(Long workflowDefinitionId);

    Optional<WorkflowTransition> findByWorkflowDefinitionIdAndFromStateAndToState(
            Long workflowDefinitionId,
            String fromState,
            String toState
    );
}
