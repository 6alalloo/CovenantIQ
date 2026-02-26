package com.covenantiq.repository;

import com.covenantiq.domain.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    Page<ActivityLog> findByLoanId(Long loanId, Pageable pageable);
    Page<ActivityLog> findByTimestampUtcBetween(OffsetDateTime start, OffsetDateTime end, Pageable pageable);
    void deleteByTimestampUtcBefore(OffsetDateTime cutoff);
}
