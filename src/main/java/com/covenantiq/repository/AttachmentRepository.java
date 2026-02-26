package com.covenantiq.repository;

import com.covenantiq.domain.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByFinancialStatementIdOrderByUploadedAtDesc(Long financialStatementId);
}
