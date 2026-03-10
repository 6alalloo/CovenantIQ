package com.covenantiq.service;

import com.covenantiq.domain.LoanImportBatch;
import com.covenantiq.domain.LoanImportRow;
import com.covenantiq.dto.request.LoanImportCsvRow;
import com.covenantiq.dto.response.LoanImportBatchResponse;
import com.covenantiq.dto.response.LoanImportExecuteResponse;
import com.covenantiq.dto.response.LoanImportPreviewResponse;
import com.covenantiq.dto.response.LoanImportRowResponse;
import com.covenantiq.enums.LoanImportBatchStatus;
import com.covenantiq.enums.LoanImportRowAction;
import com.covenantiq.exception.ResourceNotFoundException;
import com.covenantiq.exception.UnprocessableEntityException;
import com.covenantiq.repository.LoanImportBatchRepository;
import com.covenantiq.repository.LoanImportRowRepository;
import com.covenantiq.repository.LoanRepository;
import com.covenantiq.security.CurrentUserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class LoanImportService {

    private final CsvLoanImportParser csvLoanImportParser;
    private final ExternalLoanSyncService externalLoanSyncService;
    private final LoanImportBatchRepository loanImportBatchRepository;
    private final LoanImportRowRepository loanImportRowRepository;
    private final LoanRepository loanRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public LoanImportService(
            CsvLoanImportParser csvLoanImportParser,
            ExternalLoanSyncService externalLoanSyncService,
            LoanImportBatchRepository loanImportBatchRepository,
            LoanImportRowRepository loanImportRowRepository,
            LoanRepository loanRepository,
            CurrentUserService currentUserService,
            ObjectMapper objectMapper
    ) {
        this.csvLoanImportParser = csvLoanImportParser;
        this.externalLoanSyncService = externalLoanSyncService;
        this.loanImportBatchRepository = loanImportBatchRepository;
        this.loanImportRowRepository = loanImportRowRepository;
        this.loanRepository = loanRepository;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LoanImportPreviewResponse preview(MultipartFile file) {
        List<CsvLoanImportParser.ParsedRow> rows = csvLoanImportParser.parse(file);
        LoanImportBatch batch = new LoanImportBatch();
        batch.setFileName(file.getOriginalFilename() == null ? "loan-import.csv" : file.getOriginalFilename());
        batch.setUploadedBy(currentUserService.usernameOrSystem());
        batch.setStatus(LoanImportBatchStatus.PREVIEW_READY);
        batch.setTotalRows(rows.size());
        batch.setSourceSystem(detectSourceSystem(rows));
        LoanImportBatch savedBatch = loanImportBatchRepository.save(batch);

        Set<String> seenKeys = new HashSet<>();
        List<LoanImportRow> persistedRows = new ArrayList<>();
        int validRows = 0;
        int invalidRows = 0;
        int createCount = 0;
        int updateCount = 0;
        int unchangedCount = 0;

        for (CsvLoanImportParser.ParsedRow row : rows) {
            LoanImportRow importRow = new LoanImportRow();
            importRow.setBatchId(savedBatch.getId());
            importRow.setRowNumber(row.rowNumber());
            importRow.setSourceSystem(row.sourceSystem());
            importRow.setExternalLoanId(row.externalLoanId());
            importRow.setBorrowerName(row.borrowerName());
            importRow.setRawPayloadJson(toJson(row));

            if (row.validationMessage() != null) {
                importRow.setAction(LoanImportRowAction.ERROR);
                importRow.setValidationMessage(row.validationMessage());
                invalidRows++;
                persistedRows.add(importRow);
                continue;
            }

            LoanImportCsvRow payload = row.payload();
            String dedupeKey = payload.sourceSystem().toUpperCase(Locale.ROOT) + "::" + payload.externalLoanId().toUpperCase(Locale.ROOT);
            if (!seenKeys.add(dedupeKey)) {
                importRow.setAction(LoanImportRowAction.ERROR);
                importRow.setValidationMessage("Duplicate sourceSystem + externalLoanId in file");
                invalidRows++;
            } else {
                try {
                    PreviewOutcome outcome = inferAction(payload);
                    importRow.setAction(outcome.action());
                    importRow.setValidationMessage(outcome.message());
                    importRow.setLoanId(outcome.loanId());
                    validRows++;
                    if (outcome.action() == LoanImportRowAction.CREATE) createCount++;
                    if (outcome.action() == LoanImportRowAction.UPDATE) updateCount++;
                    if (outcome.action() == LoanImportRowAction.UNCHANGED) unchangedCount++;
                } catch (Exception ex) {
                    importRow.setAction(LoanImportRowAction.ERROR);
                    importRow.setValidationMessage(ex.getMessage());
                    invalidRows++;
                }
            }
            persistedRows.add(importRow);
        }

        loanImportRowRepository.saveAll(persistedRows);
        savedBatch.setValidRows(validRows);
        savedBatch.setInvalidRows(invalidRows);
        savedBatch.setCreatedCount(createCount);
        savedBatch.setUpdatedCount(updateCount);
        savedBatch.setUnchangedCount(unchangedCount);
        savedBatch.setFailedCount(invalidRows);
        loanImportBatchRepository.save(savedBatch);
        return new LoanImportPreviewResponse(toBatchResponse(savedBatch), toRowResponses(persistedRows));
    }

    @Transactional
    public LoanImportExecuteResponse execute(Long batchId) {
        LoanImportBatch batch = loanImportBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan import batch " + batchId + " not found"));
        if (batch.getStatus() != LoanImportBatchStatus.PREVIEW_READY) {
            throw new UnprocessableEntityException("Loan import batch " + batchId + " is not in PREVIEW_READY status");
        }

        List<LoanImportRow> rows = loanImportRowRepository.findByBatchIdOrderByRowNumberAsc(batchId);
        int createCount = 0;
        int updateCount = 0;
        int unchangedCount = 0;
        int failedCount = 0;

        for (LoanImportRow row : rows) {
            if (row.getAction() == LoanImportRowAction.ERROR) {
                continue;
            }
            try {
                LoanImportCsvRow payload = objectMapper.readValue(row.getRawPayloadJson(), LoanImportCsvRow.class);
                ExternalLoanSyncService.SyncResult result = externalLoanSyncService.sync(payload);
                row.setAction(result.action());
                row.setLoanId(result.loanId());
                row.setValidationMessage(result.message());
            } catch (Exception ex) {
                row.setAction(LoanImportRowAction.ERROR);
                row.setValidationMessage(ex.getMessage());
                failedCount++;
                continue;
            }
            if (row.getAction() == LoanImportRowAction.CREATE) createCount++;
            if (row.getAction() == LoanImportRowAction.UPDATE) updateCount++;
            if (row.getAction() == LoanImportRowAction.UNCHANGED) unchangedCount++;
        }

        loanImportRowRepository.saveAll(rows);
        batch.setCreatedCount(createCount);
        batch.setUpdatedCount(updateCount);
        batch.setUnchangedCount(unchangedCount);
        batch.setFailedCount(failedCount + batch.getInvalidRows());
        batch.setStatus(failedCount > 0 ? LoanImportBatchStatus.FAILED : LoanImportBatchStatus.COMPLETED);
        batch.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        loanImportBatchRepository.save(batch);
        return new LoanImportExecuteResponse(toBatchResponse(batch), toRowResponses(rows));
    }

    @Transactional(readOnly = true)
    public List<LoanImportBatchResponse> listBatches() {
        return loanImportBatchRepository.findAll().stream()
                .sorted((left, right) -> right.getStartedAt().compareTo(left.getStartedAt()))
                .map(this::toBatchResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanImportBatchResponse getBatch(Long batchId) {
        LoanImportBatch batch = loanImportBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan import batch " + batchId + " not found"));
        return toBatchResponse(batch);
    }

    @Transactional(readOnly = true)
    public List<LoanImportRowResponse> getRows(Long batchId) {
        if (!loanImportBatchRepository.existsById(batchId)) {
            throw new ResourceNotFoundException("Loan import batch " + batchId + " not found");
        }
        return toRowResponses(loanImportRowRepository.findByBatchIdOrderByRowNumberAsc(batchId));
    }

    private PreviewOutcome inferAction(LoanImportCsvRow row) {
        return loanRepository.findBySourceSystemAndExternalLoanId(row.sourceSystem(), row.externalLoanId())
                .map(existing -> {
                    if (existing.getStatus().name().equals("CLOSED") && row.status().name().equals("ACTIVE")) {
                        throw new UnprocessableEntityException(
                                "Imported loan " + row.externalLoanId() + " cannot transition from CLOSED to ACTIVE automatically"
                        );
                    }
                    boolean unchanged = existing.getBorrowerName().equals(row.borrowerName())
                            && existing.getPrincipalAmount().compareTo(row.principalAmount()) == 0
                            && existing.getStartDate().equals(row.startDate())
                            && existing.getStatus() == row.status()
                            && safeEquals(existing.getSourceUpdatedAt(), row.sourceUpdatedAt());
                    if (unchanged) {
                        return new PreviewOutcome(LoanImportRowAction.UNCHANGED, existing.getId(), null);
                    }
                    return new PreviewOutcome(LoanImportRowAction.UPDATE, existing.getId(), null);
                })
                .orElseGet(() -> new PreviewOutcome(LoanImportRowAction.CREATE, null, null));
    }

    private boolean safeEquals(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    private String detectSourceSystem(List<CsvLoanImportParser.ParsedRow> rows) {
        String first = null;
        for (CsvLoanImportParser.ParsedRow row : rows) {
            if (row.sourceSystem() != null && !row.sourceSystem().isBlank()) {
                first = row.sourceSystem();
                break;
            }
        }
        if (first == null) {
            return null;
        }
        boolean same = rows.stream()
                .map(CsvLoanImportParser.ParsedRow::sourceSystem)
                .filter(value -> value != null && !value.isBlank())
                .allMatch(first::equals);
        return same ? first : "MIXED";
    }

    private String toJson(CsvLoanImportParser.ParsedRow row) {
        try {
            if (row.payload() != null) {
                return objectMapper.writeValueAsString(row.payload());
            }
            Map<String, Object> raw = new HashMap<>();
            raw.put("rowNumber", row.rowNumber());
            raw.put("sourceSystem", row.sourceSystem());
            raw.put("externalLoanId", row.externalLoanId());
            raw.put("borrowerName", row.borrowerName());
            raw.put("validationMessage", row.validationMessage());
            return objectMapper.writeValueAsString(raw);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize import row payload");
        }
    }

    private LoanImportBatchResponse toBatchResponse(LoanImportBatch batch) {
        return new LoanImportBatchResponse(
                batch.getId(),
                batch.getFileName(),
                batch.getUploadedBy(),
                batch.getStartedAt(),
                batch.getCompletedAt(),
                batch.getStatus(),
                batch.getTotalRows(),
                batch.getValidRows(),
                batch.getInvalidRows(),
                batch.getCreatedCount(),
                batch.getUpdatedCount(),
                batch.getUnchangedCount(),
                batch.getFailedCount(),
                batch.getSourceSystem()
        );
    }

    private List<LoanImportRowResponse> toRowResponses(List<LoanImportRow> rows) {
        return rows.stream().map(row -> new LoanImportRowResponse(
                row.getId(),
                row.getRowNumber(),
                row.getSourceSystem(),
                row.getExternalLoanId(),
                row.getBorrowerName(),
                row.getAction(),
                row.getValidationMessage(),
                row.getLoanId()
        )).toList();
    }

    private record PreviewOutcome(LoanImportRowAction action, Long loanId, String message) {
    }
}
