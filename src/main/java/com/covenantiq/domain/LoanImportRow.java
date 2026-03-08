package com.covenantiq.domain;

import com.covenantiq.enums.LoanImportRowAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "loan_import_row")
public class LoanImportRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long batchId;

    @Column(nullable = false)
    private int rowNumber;

    @Column(length = 80)
    private String sourceSystem;

    @Column(length = 120)
    private String externalLoanId;

    @Column(length = 255)
    private String borrowerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LoanImportRowAction action;

    @Column(length = 2000)
    private String validationMessage;

    private Long loanId;

    @Column(nullable = false, length = 4000)
    private String rawPayloadJson;

    public Long getId() { return id; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    public String getExternalLoanId() { return externalLoanId; }
    public void setExternalLoanId(String externalLoanId) { this.externalLoanId = externalLoanId; }
    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }
    public LoanImportRowAction getAction() { return action; }
    public void setAction(LoanImportRowAction action) { this.action = action; }
    public String getValidationMessage() { return validationMessage; }
    public void setValidationMessage(String validationMessage) { this.validationMessage = validationMessage; }
    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }
    public String getRawPayloadJson() { return rawPayloadJson; }
    public void setRawPayloadJson(String rawPayloadJson) { this.rawPayloadJson = rawPayloadJson; }
}
