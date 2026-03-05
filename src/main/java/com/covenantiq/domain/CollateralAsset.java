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
@Table(name = "collateral_asset")
public class CollateralAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long loanId;

    @Column(nullable = false, length = 64)
    private String assetType;

    @Column(length = 1024)
    private String description;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal nominalValue;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal haircutPct;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal netEligibleValue;

    @Column(nullable = false)
    private Integer lienRank;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(nullable = false)
    private LocalDate effectiveDate;

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

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getNominalValue() {
        return nominalValue;
    }

    public void setNominalValue(BigDecimal nominalValue) {
        this.nominalValue = nominalValue;
    }

    public BigDecimal getHaircutPct() {
        return haircutPct;
    }

    public void setHaircutPct(BigDecimal haircutPct) {
        this.haircutPct = haircutPct;
    }

    public BigDecimal getNetEligibleValue() {
        return netEligibleValue;
    }

    public void setNetEligibleValue(BigDecimal netEligibleValue) {
        this.netEligibleValue = netEligibleValue;
    }

    public Integer getLienRank() {
        return lienRank;
    }

    public void setLienRank(Integer lienRank) {
        this.lienRank = lienRank;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
