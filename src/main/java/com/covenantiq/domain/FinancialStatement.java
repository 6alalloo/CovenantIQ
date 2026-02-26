package com.covenantiq.domain;

import com.covenantiq.enums.PeriodType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "financial_statements")
public class FinancialStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PeriodType periodType;

    @Column(nullable = false)
    private Integer fiscalYear;

    @Column
    private Integer fiscalQuarter;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentAssets;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentLiabilities;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDebt;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalEquity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal ebit;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal interestExpense;

    @Column(precision = 19, scale = 4)
    private BigDecimal netOperatingIncome;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalDebtService;

    @Column(precision = 19, scale = 4)
    private BigDecimal intangibleAssets;

    @Column(precision = 19, scale = 4)
    private BigDecimal ebitda;

    @Column(precision = 19, scale = 4)
    private BigDecimal fixedCharges;

    @Column(precision = 19, scale = 4)
    private BigDecimal inventory;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalAssets;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalLiabilities;

    @Column(nullable = false)
    private OffsetDateTime submissionTimestampUtc;

    @Column(nullable = false)
    private boolean superseded = false;

    @OneToMany(mappedBy = "financialStatement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Loan getLoan() {
        return loan;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }

    public PeriodType getPeriodType() {
        return periodType;
    }

    public void setPeriodType(PeriodType periodType) {
        this.periodType = periodType;
    }

    public Integer getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(Integer fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public Integer getFiscalQuarter() {
        return fiscalQuarter;
    }

    public void setFiscalQuarter(Integer fiscalQuarter) {
        this.fiscalQuarter = fiscalQuarter;
    }

    public BigDecimal getCurrentAssets() {
        return currentAssets;
    }

    public void setCurrentAssets(BigDecimal currentAssets) {
        this.currentAssets = currentAssets;
    }

    public BigDecimal getCurrentLiabilities() {
        return currentLiabilities;
    }

    public void setCurrentLiabilities(BigDecimal currentLiabilities) {
        this.currentLiabilities = currentLiabilities;
    }

    public BigDecimal getTotalDebt() {
        return totalDebt;
    }

    public void setTotalDebt(BigDecimal totalDebt) {
        this.totalDebt = totalDebt;
    }

    public BigDecimal getTotalEquity() {
        return totalEquity;
    }

    public void setTotalEquity(BigDecimal totalEquity) {
        this.totalEquity = totalEquity;
    }

    public BigDecimal getEbit() {
        return ebit;
    }

    public void setEbit(BigDecimal ebit) {
        this.ebit = ebit;
    }

    public BigDecimal getInterestExpense() {
        return interestExpense;
    }

    public void setInterestExpense(BigDecimal interestExpense) {
        this.interestExpense = interestExpense;
    }

    public BigDecimal getNetOperatingIncome() {
        return netOperatingIncome;
    }

    public void setNetOperatingIncome(BigDecimal netOperatingIncome) {
        this.netOperatingIncome = netOperatingIncome;
    }

    public BigDecimal getTotalDebtService() {
        return totalDebtService;
    }

    public void setTotalDebtService(BigDecimal totalDebtService) {
        this.totalDebtService = totalDebtService;
    }

    public BigDecimal getIntangibleAssets() {
        return intangibleAssets;
    }

    public void setIntangibleAssets(BigDecimal intangibleAssets) {
        this.intangibleAssets = intangibleAssets;
    }

    public BigDecimal getEbitda() {
        return ebitda;
    }

    public void setEbitda(BigDecimal ebitda) {
        this.ebitda = ebitda;
    }

    public BigDecimal getFixedCharges() {
        return fixedCharges;
    }

    public void setFixedCharges(BigDecimal fixedCharges) {
        this.fixedCharges = fixedCharges;
    }

    public BigDecimal getInventory() {
        return inventory;
    }

    public void setInventory(BigDecimal inventory) {
        this.inventory = inventory;
    }

    public BigDecimal getTotalAssets() {
        return totalAssets;
    }

    public void setTotalAssets(BigDecimal totalAssets) {
        this.totalAssets = totalAssets;
    }

    public BigDecimal getTotalLiabilities() {
        return totalLiabilities;
    }

    public void setTotalLiabilities(BigDecimal totalLiabilities) {
        this.totalLiabilities = totalLiabilities;
    }

    public OffsetDateTime getSubmissionTimestampUtc() {
        return submissionTimestampUtc;
    }

    public void setSubmissionTimestampUtc(OffsetDateTime submissionTimestampUtc) {
        this.submissionTimestampUtc = submissionTimestampUtc;
    }

    public boolean isSuperseded() {
        return superseded;
    }

    public void setSuperseded(boolean superseded) {
        this.superseded = superseded;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }
}
