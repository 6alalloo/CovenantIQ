package com.covenantiq.service;

import com.covenantiq.domain.Loan;
import com.covenantiq.dto.request.CreateLoanRequest;
import com.covenantiq.enums.LoanStatus;
import com.covenantiq.exception.ConflictException;
import com.covenantiq.exception.ResourceNotFoundException;
import com.covenantiq.repository.LoanRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {

    private final LoanRepository loanRepository;

    public LoanService(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @Transactional
    public Loan createLoan(CreateLoanRequest request) {
        Loan loan = new Loan();
        loan.setBorrowerName(request.borrowerName().trim());
        loan.setPrincipalAmount(request.principalAmount());
        loan.setStartDate(request.startDate());
        loan.setStatus(LoanStatus.ACTIVE);
        return loanRepository.save(loan);
    }

    @Transactional(readOnly = true)
    public Page<Loan> getLoans(Pageable pageable) {
        return loanRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Loan getLoan(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan " + id + " not found"));
    }

    @Transactional
    public Loan closeLoan(Long loanId) {
        Loan loan = getLoan(loanId);
        if (loan.getStatus() == LoanStatus.CLOSED) {
            throw new ConflictException("Loan " + loanId + " is already CLOSED");
        }
        loan.setStatus(LoanStatus.CLOSED);
        return loanRepository.save(loan);
    }

    public void ensureActive(Loan loan) {
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new ConflictException("Loan " + loan.getId() + " is CLOSED");
        }
    }
}
