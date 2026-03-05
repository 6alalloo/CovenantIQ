package com.covenantiq.repository;

import com.covenantiq.domain.CovenantException;
import com.covenantiq.enums.CovenantExceptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CovenantExceptionRepository extends JpaRepository<CovenantException, Long> {

    List<CovenantException> findByLoanIdOrderByIdDesc(Long loanId);

    Optional<CovenantException> findFirstByLoanIdAndCovenantIdAndStatusAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
            Long loanId,
            Long covenantId,
            CovenantExceptionStatus status,
            LocalDate from,
            LocalDate to
    );

    List<CovenantException> findByStatusAndEffectiveToBefore(CovenantExceptionStatus status, LocalDate date);
}
