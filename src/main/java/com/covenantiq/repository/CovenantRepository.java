package com.covenantiq.repository;

import com.covenantiq.domain.Covenant;
import com.covenantiq.enums.CovenantType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CovenantRepository extends JpaRepository<Covenant, Long> {
    List<Covenant> findByLoanIdOrderByIdAsc(Long loanId);

    Optional<Covenant> findByLoanIdAndType(Long loanId, CovenantType type);

    Optional<Covenant> findByIdAndLoanId(Long id, Long loanId);
}
