package com.covenantiq.service;

import com.covenantiq.domain.Alert;
import com.covenantiq.domain.CovenantResult;
import com.covenantiq.repository.AlertRepository;
import com.covenantiq.repository.CovenantResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitoringQueryService {

    private final CovenantResultRepository covenantResultRepository;
    private final AlertRepository alertRepository;
    private final LoanService loanService;

    public MonitoringQueryService(
            CovenantResultRepository covenantResultRepository,
            AlertRepository alertRepository,
            LoanService loanService
    ) {
        this.covenantResultRepository = covenantResultRepository;
        this.alertRepository = alertRepository;
        this.loanService = loanService;
    }

    @Transactional(readOnly = true)
    public Page<CovenantResult> getCovenantResults(Long loanId, Pageable pageable) {
        loanService.getLoan(loanId);
        return covenantResultRepository.findByFinancialStatementLoanIdAndSupersededFalse(loanId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Alert> getAlerts(Long loanId, Pageable pageable) {
        loanService.getLoan(loanId);
        return alertRepository.findByLoanIdAndSupersededFalse(loanId, pageable);
    }
}
