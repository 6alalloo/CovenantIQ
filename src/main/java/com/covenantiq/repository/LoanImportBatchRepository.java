package com.covenantiq.repository;

import com.covenantiq.domain.LoanImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanImportBatchRepository extends JpaRepository<LoanImportBatch, Long> {
}
