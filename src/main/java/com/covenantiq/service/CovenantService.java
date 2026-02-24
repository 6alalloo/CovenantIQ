package com.covenantiq.service;

import com.covenantiq.domain.Covenant;
import com.covenantiq.domain.Loan;
import com.covenantiq.dto.request.CreateCovenantRequest;
import com.covenantiq.exception.ConflictException;
import com.covenantiq.repository.CovenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CovenantService {

    private final CovenantRepository covenantRepository;
    private final LoanService loanService;

    public CovenantService(CovenantRepository covenantRepository, LoanService loanService) {
        this.covenantRepository = covenantRepository;
        this.loanService = loanService;
    }

    @Transactional
    public Covenant createCovenant(Long loanId, CreateCovenantRequest request) {
        Loan loan = loanService.getLoan(loanId);
        loanService.ensureActive(loan);

        covenantRepository.findByLoanIdAndType(loanId, request.type()).ifPresent(existing -> {
            throw new ConflictException("Loan " + loanId + " already has covenant type " + request.type());
        });

        Covenant covenant = new Covenant();
        covenant.setLoan(loan);
        covenant.setType(request.type());
        covenant.setThresholdValue(request.thresholdValue());
        covenant.setComparisonType(request.comparisonType());
        covenant.setSeverityLevel(request.severityLevel());
        return covenantRepository.save(covenant);
    }

    @Transactional(readOnly = true)
    public List<Covenant> getLoanCovenants(Long loanId) {
        return covenantRepository.findByLoanIdOrderByIdAsc(loanId);
    }
}
