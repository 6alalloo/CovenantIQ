package com.covenantiq.controller;

import com.covenantiq.dto.request.CreateCovenantRequest;
import com.covenantiq.dto.request.CreateCommentRequest;
import com.covenantiq.dto.request.CreateLoanRequest;
import com.covenantiq.dto.request.SubmitFinancialStatementRequest;
import com.covenantiq.dto.request.UpdateCovenantRequest;
import com.covenantiq.dto.response.AlertResponse;
import com.covenantiq.dto.response.BulkImportSummaryResponse;
import com.covenantiq.dto.response.CommentResponse;
import com.covenantiq.dto.response.CovenantResponse;
import com.covenantiq.dto.response.CovenantResultResponse;
import com.covenantiq.dto.response.FinancialStatementResponse;
import com.covenantiq.dto.response.LoanResponse;
import com.covenantiq.dto.response.RiskDetailsResponse;
import com.covenantiq.dto.response.RiskSummaryResponse;
import com.covenantiq.mapper.ResponseMapper;
import com.covenantiq.service.BulkImportService;
import com.covenantiq.service.CovenantService;
import com.covenantiq.service.CommentService;
import com.covenantiq.service.ExportService;
import com.covenantiq.service.FinancialStatementService;
import com.covenantiq.service.LoanService;
import com.covenantiq.service.MonitoringQueryService;
import com.covenantiq.service.RiskSummaryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    private final LoanService loanService;
    private final CovenantService covenantService;
    private final FinancialStatementService financialStatementService;
    private final MonitoringQueryService monitoringQueryService;
    private final RiskSummaryService riskSummaryService;
    private final ExportService exportService;
    private final BulkImportService bulkImportService;
    private final CommentService commentService;

    public LoanController(
            LoanService loanService,
            CovenantService covenantService,
            FinancialStatementService financialStatementService,
            MonitoringQueryService monitoringQueryService,
            RiskSummaryService riskSummaryService,
            ExportService exportService,
            BulkImportService bulkImportService,
            CommentService commentService
    ) {
        this.loanService = loanService;
        this.covenantService = covenantService;
        this.financialStatementService = financialStatementService;
        this.monitoringQueryService = monitoringQueryService;
        this.riskSummaryService = riskSummaryService;
        this.exportService = exportService;
        this.bulkImportService = bulkImportService;
        this.commentService = commentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public LoanResponse createLoan(@Valid @RequestBody CreateLoanRequest request) {
        return ResponseMapper.toLoanResponse(loanService.createLoan(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public Page<LoanResponse> getLoans(Pageable pageable, @RequestParam(value = "q", required = false) String q) {
        return loanService.getLoans(pageable, q).map(ResponseMapper::toLoanResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public LoanResponse getLoan(@PathVariable Long id) {
        return ResponseMapper.toLoanResponse(loanService.getLoan(id));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public LoanResponse closeLoan(@PathVariable Long id) {
        return ResponseMapper.toLoanResponse(loanService.closeLoan(id));
    }

    @PostMapping("/{loanId}/covenants")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public CovenantResponse addCovenant(@PathVariable Long loanId, @Valid @RequestBody CreateCovenantRequest request) {
        return ResponseMapper.toCovenantResponse(covenantService.createCovenant(loanId, request));
    }

    @PatchMapping("/{loanId}/covenants/{covenantId}")
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public CovenantResponse updateCovenant(
            @PathVariable Long loanId,
            @PathVariable Long covenantId,
            @Valid @RequestBody UpdateCovenantRequest request
    ) {
        return ResponseMapper.toCovenantResponse(covenantService.updateCovenant(loanId, covenantId, request));
    }

    @GetMapping("/{loanId}/covenants")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public List<CovenantResponse> getCovenants(@PathVariable Long loanId) {
        return covenantService.getLoanCovenants(loanId).stream()
                .map(ResponseMapper::toCovenantResponse)
                .toList();
    }

    @PostMapping("/{loanId}/financial-statements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public FinancialStatementResponse submitFinancialStatement(
            @PathVariable Long loanId,
            @Valid @RequestBody SubmitFinancialStatementRequest request
    ) {
        return ResponseMapper.toFinancialStatementResponse(financialStatementService.submitStatement(loanId, request));
    }

    @PostMapping(value = "/{loanId}/financial-statements/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public BulkImportSummaryResponse bulkImportStatements(
            @PathVariable Long loanId,
            @RequestPart("file") MultipartFile file
    ) {
        return bulkImportService.importStatements(loanId, file);
    }

    @PostMapping("/{loanId}/comments")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public CommentResponse addComment(@PathVariable Long loanId, @Valid @RequestBody CreateCommentRequest request) {
        return commentService.addComment(loanId, request.commentText());
    }

    @GetMapping("/{loanId}/comments")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public Page<CommentResponse> getComments(@PathVariable Long loanId, Pageable pageable) {
        return commentService.getComments(loanId, pageable);
    }

    @DeleteMapping("/{loanId}/comments/{commentId}")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public ResponseEntity<Void> deleteComment(@PathVariable Long loanId, @PathVariable Long commentId) {
        commentService.deleteComment(loanId, commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{loanId}/covenant-results")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public Page<CovenantResultResponse> getCovenantResults(@PathVariable Long loanId, Pageable pageable) {
        return monitoringQueryService.getCovenantResults(loanId, pageable).map(ResponseMapper::toCovenantResultResponse);
    }

    @GetMapping("/{loanId}/alerts")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public Page<AlertResponse> getAlerts(@PathVariable Long loanId, Pageable pageable) {
        return monitoringQueryService.getAlerts(loanId, pageable).map(ResponseMapper::toAlertResponse);
    }

    @GetMapping("/{loanId}/risk-summary")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public RiskSummaryResponse getRiskSummary(@PathVariable Long loanId) {
        return riskSummaryService.getRiskSummary(loanId);
    }

    @GetMapping("/{loanId}/risk-details")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public RiskDetailsResponse getRiskDetails(@PathVariable Long loanId) {
        return riskSummaryService.getRiskDetails(loanId);
    }

    @GetMapping(value = "/{loanId}/alerts/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public ResponseEntity<String> exportAlerts(@PathVariable Long loanId) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header("Content-Disposition", "attachment; filename=\"loan-" + loanId + "-alerts.csv\"")
                .body(exportService.exportAlertsCsv(loanId));
    }

    @GetMapping(value = "/{loanId}/covenant-results/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public ResponseEntity<String> exportCovenantResults(@PathVariable Long loanId) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header("Content-Disposition", "attachment; filename=\"loan-" + loanId + "-covenant-results.csv\"")
                .body(exportService.exportCovenantResultsCsv(loanId));
    }
}
