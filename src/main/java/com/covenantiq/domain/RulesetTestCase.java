package com.covenantiq.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "ruleset_test_case")
public class RulesetTestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long rulesetVersionId;

    @Lob
    @Column(nullable = false)
    private String inputJson;

    @Lob
    @Column(nullable = false)
    private String expectedOutputJson;

    @Lob
    private String actualOutputJson;

    private Boolean pass;

    private OffsetDateTime executedAt;

    @PrePersist
    void prePersist() {
        if (executedAt == null) {
            executedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getRulesetVersionId() {
        return rulesetVersionId;
    }

    public void setRulesetVersionId(Long rulesetVersionId) {
        this.rulesetVersionId = rulesetVersionId;
    }

    public String getInputJson() {
        return inputJson;
    }

    public void setInputJson(String inputJson) {
        this.inputJson = inputJson;
    }

    public String getExpectedOutputJson() {
        return expectedOutputJson;
    }

    public void setExpectedOutputJson(String expectedOutputJson) {
        this.expectedOutputJson = expectedOutputJson;
    }

    public String getActualOutputJson() {
        return actualOutputJson;
    }

    public void setActualOutputJson(String actualOutputJson) {
        this.actualOutputJson = actualOutputJson;
    }

    public Boolean getPass() {
        return pass;
    }

    public void setPass(Boolean pass) {
        this.pass = pass;
    }

    public OffsetDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(OffsetDateTime executedAt) {
        this.executedAt = executedAt;
    }
}
