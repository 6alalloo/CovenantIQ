package com.covenantiq.repository;

import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.enums.PeriodType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinancialStatementRepository extends JpaRepository<FinancialStatement, Long> {

    Optional<FinancialStatement> findByLoanIdAndPeriodTypeAndFiscalYearAndFiscalQuarterAndSupersededFalse(
            Long loanId, PeriodType periodType, Integer fiscalYear, Integer fiscalQuarter
    );

    List<FinancialStatement> findByLoanIdAndSupersededFalseOrderByFiscalYearAscFiscalQuarterAscIdAsc(Long loanId);

    List<FinancialStatement> findByLoanIdAndPeriodTypeAndSupersededFalseOrderByFiscalYearAscFiscalQuarterAscIdAsc(
            Long loanId, PeriodType periodType
    );

    Optional<FinancialStatement> findTopByLoanIdAndSupersededFalseOrderBySubmissionTimestampUtcDescIdDesc(Long loanId);
}
