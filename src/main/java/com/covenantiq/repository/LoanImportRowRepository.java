package com.covenantiq.repository;

import com.covenantiq.domain.LoanImportRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanImportRowRepository extends JpaRepository<LoanImportRow, Long> {
    List<LoanImportRow> findByBatchIdOrderByRowNumberAsc(Long batchId);
}
