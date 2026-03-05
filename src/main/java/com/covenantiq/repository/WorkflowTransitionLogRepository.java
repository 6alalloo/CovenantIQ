package com.covenantiq.repository;

import com.covenantiq.domain.WorkflowTransitionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowTransitionLogRepository extends JpaRepository<WorkflowTransitionLog, Long> {

    List<WorkflowTransitionLog> findByWorkflowInstanceIdOrderByTimestampUtcDesc(Long workflowInstanceId);
}
