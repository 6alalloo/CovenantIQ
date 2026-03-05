package com.covenantiq.repository;

import com.covenantiq.domain.WorkflowState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowStateRepository extends JpaRepository<WorkflowState, Long> {

    List<WorkflowState> findByWorkflowDefinitionIdOrderByIdAsc(Long workflowDefinitionId);

    Optional<WorkflowState> findByWorkflowDefinitionIdAndStateCode(Long workflowDefinitionId, String stateCode);

    Optional<WorkflowState> findByWorkflowDefinitionIdAndInitialStateTrue(Long workflowDefinitionId);
}
