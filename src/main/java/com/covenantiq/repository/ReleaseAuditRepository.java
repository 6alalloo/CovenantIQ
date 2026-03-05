package com.covenantiq.repository;

import com.covenantiq.domain.ReleaseAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReleaseAuditRepository extends JpaRepository<ReleaseAudit, Long> {

    List<ReleaseAudit> findByReleaseBatchIdOrderByTimestampUtcDesc(Long releaseBatchId);
}
