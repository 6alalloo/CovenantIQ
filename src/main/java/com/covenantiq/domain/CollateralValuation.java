package com.covenantiq.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "collateral_valuation")
public class CollateralValuation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long collateralAssetId;

    @Column(nullable = false)
    private LocalDate valuationDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal valuedAmount;

    @Column(nullable = false, length = 80)
    private String method;

    @Column(length = 120)
    private String source;

    @Column(precision = 8, scale = 4)
    private BigDecimal confidence;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public Long getCollateralAssetId() {
        return collateralAssetId;
    }

    public void setCollateralAssetId(Long collateralAssetId) {
        this.collateralAssetId = collateralAssetId;
    }

    public LocalDate getValuationDate() {
        return valuationDate;
    }

    public void setValuationDate(LocalDate valuationDate) {
        this.valuationDate = valuationDate;
    }

    public BigDecimal getValuedAmount() {
        return valuedAmount;
    }

    public void setValuedAmount(BigDecimal valuedAmount) {
        this.valuedAmount = valuedAmount;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
