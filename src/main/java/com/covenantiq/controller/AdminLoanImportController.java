package com.covenantiq.controller;

import com.covenantiq.dto.response.LoanImportBatchResponse;
import com.covenantiq.dto.response.LoanImportExecuteResponse;
import com.covenantiq.dto.response.LoanImportPreviewResponse;
import com.covenantiq.dto.response.LoanImportRowResponse;
import com.covenantiq.service.LoanImportService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/loan-imports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLoanImportController {

    private final LoanImportService loanImportService;

    public AdminLoanImportController(LoanImportService loanImportService) {
        this.loanImportService = loanImportService;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LoanImportPreviewResponse preview(@RequestPart("file") MultipartFile file) {
        return loanImportService.preview(file);
    }

    @PostMapping("/{batchId}/execute")
    public LoanImportExecuteResponse execute(@PathVariable Long batchId) {
        return loanImportService.execute(batchId);
    }

    @GetMapping
    public List<LoanImportBatchResponse> list() {
        return loanImportService.listBatches();
    }

    @GetMapping("/{batchId}")
    public LoanImportBatchResponse getBatch(@PathVariable Long batchId) {
        return loanImportService.getBatch(batchId);
    }

    @GetMapping("/{batchId}/rows")
    public List<LoanImportRowResponse> getRows(@PathVariable Long batchId) {
        return loanImportService.getRows(batchId);
    }
}
