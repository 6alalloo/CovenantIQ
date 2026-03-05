package com.covenantiq.domain;

import com.covenantiq.enums.CovenantExceptionStatus;
import com.covenantiq.enums.CovenantExceptionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "covenant_exception")
public class CovenantException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long loanId;

    @Column(nullable = false)
    private Long covenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CovenantExceptionType exceptionType;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    @Column(nullable = false)
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CovenantExceptionStatus status;

    @Column(nullable = false)
    private String requestedBy;

    private String approvedBy;

    private OffsetDateTime approvedAt;

    @Lob
    @Column(nullable = false)
    private String controlsJson = "{}";

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public Long getCovenantId() {
        return covenantId;
    }

    public void setCovenantId(Long covenantId) {
        this.covenantId = covenantId;
    }

    public CovenantExceptionType getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(CovenantExceptionType exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public CovenantExceptionStatus getStatus() {
        return status;
    }

    public void setStatus(CovenantExceptionStatus status) {
        this.status = status;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getControlsJson() {
        return controlsJson;
    }

    public void setControlsJson(String controlsJson) {
        this.controlsJson = controlsJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
