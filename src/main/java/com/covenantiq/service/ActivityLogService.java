package com.covenantiq.service;

import com.covenantiq.domain.ActivityLog;
import com.covenantiq.dto.response.ActivityLogResponse;
import com.covenantiq.enums.ActivityEventType;
import com.covenantiq.repository.ActivityLogRepository;
import com.covenantiq.security.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final CurrentUserService currentUserService;

    public ActivityLogService(ActivityLogRepository activityLogRepository, CurrentUserService currentUserService) {
        this.activityLogRepository = activityLogRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public void logEvent(
            ActivityEventType eventType,
            String entityType,
            Long entityId,
            Long loanId,
            String description
    ) {
        ActivityLog log = new ActivityLog();
        log.setEventType(eventType);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setLoanId(loanId);
        log.setDescription(description);
        log.setUsername(currentUserService.usernameOrSystem());
        log.setTimestampUtc(OffsetDateTime.now(ZoneOffset.UTC));
        activityLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getActivityForLoan(Long loanId, Pageable pageable) {
        return activityLogRepository.findByLoanId(loanId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getActivityForDateRange(LocalDate start, LocalDate end, Pageable pageable) {
        OffsetDateTime startTs = start.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endTs = end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1);
        return activityLogRepository.findByTimestampUtcBetween(startTs, endTs, pageable).map(this::toResponse);
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldLogs() {
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(90);
        activityLogRepository.deleteByTimestampUtcBefore(cutoff);
    }

    private ActivityLogResponse toResponse(ActivityLog log) {
        return new ActivityLogResponse(
                log.getId(),
                log.getEventType(),
                log.getEntityType(),
                log.getEntityId(),
                log.getUsername(),
                log.getTimestampUtc(),
                log.getDescription(),
                log.getLoanId()
        );
    }
}
