package com.covenantiq.repository;

import com.covenantiq.domain.WorkflowDefinition;
import com.covenantiq.enums.WorkflowDefinitionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {

    List<WorkflowDefinition> findByEntityTypeOrderByVersionDesc(String entityType);

    Optional<WorkflowDefinition> findByEntityTypeAndStatus(String entityType, WorkflowDefinitionStatus status);

    int countByEntityType(String entityType);
}
