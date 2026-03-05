package com.covenantiq.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "workflow_transition")
public class WorkflowTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long workflowDefinitionId;

    @Column(nullable = false, length = 64)
    private String fromState;

    @Column(nullable = false, length = 64)
    private String toState;

    @Column(nullable = false, length = 512)
    private String allowedRolesCsv;

    @Lob
    @Column(nullable = false)
    private String requiredFieldsJson = "[]";

    @Column(length = 256)
    private String guardExpression;

    public Long getId() {
        return id;
    }

    public Long getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    public void setWorkflowDefinitionId(Long workflowDefinitionId) {
        this.workflowDefinitionId = workflowDefinitionId;
    }

    public String getFromState() {
        return fromState;
    }

    public void setFromState(String fromState) {
        this.fromState = fromState;
    }

    public String getToState() {
        return toState;
    }

    public void setToState(String toState) {
        this.toState = toState;
    }

    public String getAllowedRolesCsv() {
        return allowedRolesCsv;
    }

    public void setAllowedRolesCsv(String allowedRolesCsv) {
        this.allowedRolesCsv = allowedRolesCsv;
    }

    public String getRequiredFieldsJson() {
        return requiredFieldsJson;
    }

    public void setRequiredFieldsJson(String requiredFieldsJson) {
        this.requiredFieldsJson = requiredFieldsJson;
    }

    public String getGuardExpression() {
        return guardExpression;
    }

    public void setGuardExpression(String guardExpression) {
        this.guardExpression = guardExpression;
    }
}
