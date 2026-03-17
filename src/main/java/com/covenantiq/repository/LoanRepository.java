package com.covenantiq.repository;

import com.covenantiq.domain.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    Optional<Loan> findBySourceSystemAndExternalLoanId(String sourceSystem, String externalLoanId);

    @Query("""
            select l
            from Loan l
            where :q is null
               or :q = ''
               or lower(l.borrowerName) like lower(concat('%', :q, '%'))
               or str(l.id) like concat('%', :q, '%')
            """)
    Page<Loan> searchLoans(@Param("q") String q, Pageable pageable);
}
