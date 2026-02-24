package com.covenantiq.repository;

import com.covenantiq.domain.CovenantResult;
import com.covenantiq.enums.CovenantResultStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CovenantResultRepository extends JpaRepository<CovenantResult, Long> {
    List<CovenantResult> findByFinancialStatementIdAndSupersededFalse(Long financialStatementId);

    Page<CovenantResult> findByFinancialStatementLoanIdAndSupersededFalse(Long loanId, Pageable pageable);

    long countByFinancialStatementIdAndStatusAndSupersededFalse(Long financialStatementId, CovenantResultStatus status);
}
