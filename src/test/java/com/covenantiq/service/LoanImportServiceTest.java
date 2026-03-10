package com.covenantiq.service;

import com.covenantiq.domain.Loan;
import com.covenantiq.domain.LoanImportBatch;
import com.covenantiq.domain.LoanImportRow;
import com.covenantiq.dto.request.LoanImportCsvRow;
import com.covenantiq.dto.response.LoanImportExecuteResponse;
import com.covenantiq.enums.LoanImportBatchStatus;
import com.covenantiq.enums.LoanImportRowAction;
import com.covenantiq.enums.LoanStatus;
import com.covenantiq.exception.UnsupportedFileTypeException;
import com.covenantiq.repository.LoanImportBatchRepository;
import com.covenantiq.repository.LoanImportRowRepository;
import com.covenantiq.repository.LoanRepository;
import com.covenantiq.security.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanImportServiceTest {

    @Mock
    private ExternalLoanSyncService externalLoanSyncService;

    @Mock
    private LoanImportBatchRepository loanImportBatchRepository;

    @Mock
    private LoanImportRowRepository loanImportRowRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private CurrentUserService currentUserService;

    private LoanImportService loanImportService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        loanImportService = new LoanImportService(
                new CsvLoanImportParser(),
                externalLoanSyncService,
                loanImportBatchRepository,
                loanImportRowRepository,
                loanRepository,
                currentUserService,
                objectMapper
        );
    }

    @Test
    void previewRejectsMalformedCsv() throws IOException {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(128L);
        when(file.getOriginalFilename()).thenReturn("loan-import.csv");
        when(file.getInputStream()).thenThrow(new IOException("boom"));

        UnsupportedFileTypeException ex = assertThrows(UnsupportedFileTypeException.class, () -> loanImportService.preview(file));

        assertEquals("Unable to read import CSV", ex.getMessage());
        verifyNoInteractions(loanImportBatchRepository, loanImportRowRepository, externalLoanSyncService, loanRepository, currentUserService);
    }

    @Test
    void previewRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "loan-import.csv", "text/csv", new byte[0]);

        UnsupportedFileTypeException ex = assertThrows(UnsupportedFileTypeException.class, () -> loanImportService.preview(file));

        assertEquals("CSV file is required", ex.getMessage());
        verifyNoInteractions(loanImportBatchRepository, loanImportRowRepository, externalLoanSyncService, loanRepository, currentUserService);
    }

    @Test
    void previewRejectsMissingRequiredHeaders() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "loan-import.csv",
                "text/csv",
                (
                        "sourceSystem,externalLoanId,borrowerName,principalAmount,startDate\n" +
                        "CORE_BANKING,LN-100001,Borrower,1250000.00,2025-01-15\n"
                ).getBytes()
        );

        UnsupportedFileTypeException ex = assertThrows(UnsupportedFileTypeException.class, () -> loanImportService.preview(file));

        assertEquals("Missing required column: status", ex.getMessage());
        verifyNoInteractions(loanImportBatchRepository, loanImportRowRepository, externalLoanSyncService, loanRepository, currentUserService);
    }

    @Test
    void previewFlagsInvalidRowValues() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "loan-import.csv",
                "text/csv",
                (
                        "sourceSystem,externalLoanId,borrowerName,principalAmount,startDate,status,sourceUpdatedAt\n" +
                        "CORE_BANKING,LN-100009,Invalid Decimal,not-a-number,2025-01-15,ACTIVE,2026-03-08T10:15:00Z\n"
                ).getBytes()
        );
        LoanImportBatch savedBatch = previewBatch();

        when(currentUserService.usernameOrSystem()).thenReturn("admin@demo.com");
        when(loanImportBatchRepository.save(any(LoanImportBatch.class))).thenReturn(savedBatch);
        when(loanImportRowRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = loanImportService.preview(file);

        assertEquals(LoanImportRowAction.ERROR, response.rows().get(0).action());
        assertEquals("Invalid decimal value for principalAmount", response.rows().get(0).validationMessage());
        assertEquals(0, response.batch().validRows());
        assertEquals(1, response.batch().invalidRows());
        assertEquals(1, response.batch().failedCount());
    }

    @Test
    void previewMarksExistingLoanAsUnchangedWhenImportedDataMatches() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "loan-import.csv",
                "text/csv",
                (
                        "sourceSystem,externalLoanId,borrowerName,principalAmount,startDate,status,sourceUpdatedAt\n" +
                        "CORE_BANKING,LN-300001,Same Borrower,1250000.00,2025-01-15,ACTIVE,2026-03-08T10:15:00Z\n"
                ).getBytes()
        );
        Loan existing = new Loan();
        existing.setId(301L);
        existing.setSourceSystem("CORE_BANKING");
        existing.setExternalLoanId("LN-300001");
        existing.setBorrowerName("Same Borrower");
        existing.setPrincipalAmount(new BigDecimal("1250000.00"));
        existing.setStartDate(LocalDate.parse("2025-01-15"));
        existing.setStatus(LoanStatus.ACTIVE);
        existing.setSourceUpdatedAt(OffsetDateTime.parse("2026-03-08T10:15:00Z"));
        LoanImportBatch savedBatch = previewBatch();

        when(currentUserService.usernameOrSystem()).thenReturn("admin@demo.com");
        when(loanImportBatchRepository.save(any(LoanImportBatch.class))).thenReturn(savedBatch);
        when(loanImportRowRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(loanRepository.findBySourceSystemAndExternalLoanId("CORE_BANKING", "LN-300001"))
                .thenReturn(Optional.of(existing));

        var response = loanImportService.preview(file);

        assertEquals(LoanImportRowAction.UNCHANGED, response.rows().get(0).action());
        assertEquals(301L, response.rows().get(0).loanId());
        assertEquals(1, response.batch().unchangedCount());
        assertEquals(0, response.batch().createdCount());
        assertEquals(0, response.batch().updatedCount());
    }

    @Test
    void previewMarksExistingLoanAsUpdateWhenImportedDataDiffers() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "loan-import.csv",
                "text/csv",
                (
                        "sourceSystem,externalLoanId,borrowerName,principalAmount,startDate,status,sourceUpdatedAt\n" +
                        "CORE_BANKING,LN-300002,Updated Borrower,1250000.00,2025-01-15,ACTIVE,2026-03-08T10:15:00Z\n"
                ).getBytes()
        );
        Loan existing = new Loan();
        existing.setId(302L);
        existing.setSourceSystem("CORE_BANKING");
        existing.setExternalLoanId("LN-300002");
        existing.setBorrowerName("Original Borrower");
        existing.setPrincipalAmount(new BigDecimal("1000000.00"));
        existing.setStartDate(LocalDate.parse("2025-01-15"));
        existing.setStatus(LoanStatus.ACTIVE);
        existing.setSourceUpdatedAt(OffsetDateTime.parse("2026-03-01T00:00:00Z"));
        LoanImportBatch savedBatch = previewBatch();

        when(currentUserService.usernameOrSystem()).thenReturn("admin@demo.com");
        when(loanImportBatchRepository.save(any(LoanImportBatch.class))).thenReturn(savedBatch);
        when(loanImportRowRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(loanRepository.findBySourceSystemAndExternalLoanId("CORE_BANKING", "LN-300002"))
                .thenReturn(Optional.of(existing));

        var response = loanImportService.preview(file);

        assertEquals(LoanImportRowAction.UPDATE, response.rows().get(0).action());
        assertEquals(302L, response.rows().get(0).loanId());
        assertEquals(1, response.batch().updatedCount());
        assertEquals(0, response.batch().createdCount());
        assertEquals(0, response.batch().unchangedCount());
    }

    @Test
    void executeMarksRowsUnchangedWhenNoDataChangesExist() throws Exception {
        LoanImportBatch batch = new LoanImportBatch();
        batch.setFileName("loan-import.csv");
        batch.setUploadedBy("admin@demo.com");
        batch.setStatus(LoanImportBatchStatus.PREVIEW_READY);
        batch.setTotalRows(1);
        batch.setValidRows(1);
        batch.setInvalidRows(0);

        LoanImportCsvRow payload = new LoanImportCsvRow(
                2,
                "CORE_BANKING",
                "LN-300003",
                "Same Borrower",
                new BigDecimal("1250000.00"),
                LocalDate.parse("2025-01-15"),
                LoanStatus.ACTIVE,
                OffsetDateTime.parse("2026-03-08T10:15:00Z")
        );
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        LoanImportRow row = new LoanImportRow();
        row.setBatchId(11L);
        row.setRowNumber(2);
        row.setSourceSystem(payload.sourceSystem());
        row.setExternalLoanId(payload.externalLoanId());
        row.setBorrowerName(payload.borrowerName());
        row.setAction(LoanImportRowAction.UNCHANGED);
        row.setRawPayloadJson(objectMapper.writeValueAsString(payload));

        when(loanImportBatchRepository.findById(11L)).thenReturn(Optional.of(batch));
        when(loanImportRowRepository.findByBatchIdOrderByRowNumberAsc(11L)).thenReturn(List.of(row));
        when(externalLoanSyncService.sync(any(LoanImportCsvRow.class)))
                .thenReturn(new ExternalLoanSyncService.SyncResult(LoanImportRowAction.UNCHANGED, 303L, null));
        when(loanImportRowRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(loanImportBatchRepository.save(any(LoanImportBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanImportExecuteResponse response = loanImportService.execute(11L);

        assertEquals(LoanImportBatchStatus.COMPLETED, response.batch().status());
        assertEquals(0, response.batch().createdCount());
        assertEquals(0, response.batch().updatedCount());
        assertEquals(1, response.batch().unchangedCount());
        assertEquals(0, response.batch().failedCount());
        assertEquals(LoanImportRowAction.UNCHANGED, response.rows().get(0).action());
        assertEquals(303L, response.rows().get(0).loanId());
    }

    @Test
    void executeProcessesValidRowsAndReportsInvalidRows() throws Exception {
        LoanImportBatch batch = new LoanImportBatch();
        batch.setFileName("loan-import.csv");
        batch.setUploadedBy("admin@demo.com");
        batch.setStatus(LoanImportBatchStatus.PREVIEW_READY);
        batch.setTotalRows(2);
        batch.setValidRows(1);
        batch.setInvalidRows(1);

        LoanImportCsvRow validPayload = new LoanImportCsvRow(
                2,
                "CORE_BANKING",
                "LN-200001",
                "Imported Demo Borrower",
                new BigDecimal("1250000.00"),
                LocalDate.parse("2025-01-15"),
                LoanStatus.ACTIVE,
                OffsetDateTime.parse("2026-03-08T10:15:00Z")
        );
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        LoanImportRow validRow = new LoanImportRow();
        validRow.setBatchId(10L);
        validRow.setRowNumber(2);
        validRow.setSourceSystem(validPayload.sourceSystem());
        validRow.setExternalLoanId(validPayload.externalLoanId());
        validRow.setBorrowerName(validPayload.borrowerName());
        validRow.setAction(LoanImportRowAction.CREATE);
        validRow.setRawPayloadJson(objectMapper.writeValueAsString(validPayload));

        LoanImportRow invalidRow = new LoanImportRow();
        invalidRow.setBatchId(10L);
        invalidRow.setRowNumber(3);
        invalidRow.setSourceSystem("CORE_BANKING");
        invalidRow.setExternalLoanId("LN-200001");
        invalidRow.setBorrowerName("Duplicate Borrower");
        invalidRow.setAction(LoanImportRowAction.ERROR);
        invalidRow.setValidationMessage("Duplicate sourceSystem + externalLoanId in file");
        invalidRow.setRawPayloadJson("{}");

        when(loanImportBatchRepository.findById(10L)).thenReturn(Optional.of(batch));
        when(loanImportRowRepository.findByBatchIdOrderByRowNumberAsc(10L)).thenReturn(List.of(validRow, invalidRow));
        when(externalLoanSyncService.sync(any(LoanImportCsvRow.class)))
                .thenReturn(new ExternalLoanSyncService.SyncResult(LoanImportRowAction.CREATE, 101L, null));
        when(loanImportRowRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(loanImportBatchRepository.save(any(LoanImportBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanImportExecuteResponse response = loanImportService.execute(10L);

        assertEquals(LoanImportBatchStatus.COMPLETED, response.batch().status());
        assertEquals(1, response.batch().createdCount());
        assertEquals(0, response.batch().updatedCount());
        assertEquals(0, response.batch().unchangedCount());
        assertEquals(1, response.batch().failedCount());
        assertEquals(2, response.rows().size());
        assertEquals(LoanImportRowAction.CREATE, response.rows().get(0).action());
        assertEquals(101L, response.rows().get(0).loanId());
        assertEquals(LoanImportRowAction.ERROR, response.rows().get(1).action());
        assertTrue(response.batch().completedAt() != null);

        verify(externalLoanSyncService).sync(any(LoanImportCsvRow.class));
        verify(loanImportRowRepository).saveAll(any());
        verify(loanImportBatchRepository).save(batch);
        verify(loanRepository, never()).findBySourceSystemAndExternalLoanId(any(), any());
    }

    private LoanImportBatch previewBatch() {
        LoanImportBatch batch = new LoanImportBatch();
        batch.setFileName("loan-import.csv");
        batch.setUploadedBy("admin@demo.com");
        batch.setStatus(LoanImportBatchStatus.PREVIEW_READY);
        batch.setTotalRows(1);
        batch.setValidRows(1);
        batch.setInvalidRows(0);
        return batch;
    }
}
