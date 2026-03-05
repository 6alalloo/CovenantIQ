package com.covenantiq.repository;

import com.covenantiq.domain.EventOutbox;
import com.covenantiq.enums.EventOutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface EventOutboxRepository extends JpaRepository<EventOutbox, Long> {

    List<EventOutbox> findByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(
            List<EventOutboxStatus> statuses,
            OffsetDateTime nextAttemptAt,
            Pageable pageable
    );

    long countByStatus(EventOutboxStatus status);

    Optional<EventOutbox> findByEventId(String eventId);
}
