package com.covenantiq.domain;

import com.covenantiq.enums.CovenantResultStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "covenant_results")
public class CovenantResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "covenant_id", nullable = false)
    private Covenant covenant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "financial_statement_id", nullable = false)
    private FinancialStatement financialStatement;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal actualValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CovenantResultStatus status;

    @Column(nullable = false)
    private OffsetDateTime evaluationTimestampUtc;

    @Column(nullable = false)
    private boolean superseded = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Covenant getCovenant() {
        return covenant;
    }

    public void setCovenant(Covenant covenant) {
        this.covenant = covenant;
    }

    public FinancialStatement getFinancialStatement() {
        return financialStatement;
    }

    public void setFinancialStatement(FinancialStatement financialStatement) {
        this.financialStatement = financialStatement;
    }

    public BigDecimal getActualValue() {
        return actualValue;
    }

    public void setActualValue(BigDecimal actualValue) {
        this.actualValue = actualValue;
    }

    public CovenantResultStatus getStatus() {
        return status;
    }

    public void setStatus(CovenantResultStatus status) {
        this.status = status;
    }

    public OffsetDateTime getEvaluationTimestampUtc() {
        return evaluationTimestampUtc;
    }

    public void setEvaluationTimestampUtc(OffsetDateTime evaluationTimestampUtc) {
        this.evaluationTimestampUtc = evaluationTimestampUtc;
    }

    public boolean isSuperseded() {
        return superseded;
    }

    public void setSuperseded(boolean superseded) {
        this.superseded = superseded;
    }
}
