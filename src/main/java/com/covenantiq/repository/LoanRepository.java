package com.covenantiq.repository;

import com.covenantiq.domain.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    Optional<Loan> findBySourceSystemAndExternalLoanId(String sourceSystem, String externalLoanId);
}
