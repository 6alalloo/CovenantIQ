package com.covenantiq.service;

import com.covenantiq.domain.ReleaseBatch;
import com.covenantiq.dto.request.RollbackReleaseRequest;
import com.covenantiq.exception.UnprocessableEntityException;
import com.covenantiq.repository.ChangeRequestItemRepository;
import com.covenantiq.repository.ChangeRequestRepository;
import com.covenantiq.repository.ReleaseAuditRepository;
import com.covenantiq.repository.ReleaseBatchRepository;
import com.covenantiq.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeControlServiceTest {

    @Mock
    private ChangeRequestRepository changeRequestRepository;

    @Mock
    private ChangeRequestItemRepository changeRequestItemRepository;

    @Mock
    private ReleaseBatchRepository releaseBatchRepository;

    @Mock
    private ReleaseAuditRepository releaseAuditRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @Test
    void rollbackRejectsTargetFromDifferentChangeRequestLineage() {
        ChangeControlService service = new ChangeControlService(
                changeRequestRepository,
                changeRequestItemRepository,
                releaseBatchRepository,
                releaseAuditRepository,
                currentUserService,
                outboxEventPublisher
        );
        ReleaseBatch sourceRelease = new ReleaseBatch();
        sourceRelease.setChangeRequestId(101L);
        sourceRelease.setReleaseTag("v1.2.3");

        ReleaseBatch targetRelease = new ReleaseBatch();
        targetRelease.setChangeRequestId(202L);
        targetRelease.setReleaseTag("v9.9.9");

        when(releaseBatchRepository.findById(1L)).thenReturn(Optional.of(sourceRelease));
        when(releaseBatchRepository.findById(2L)).thenReturn(Optional.of(targetRelease));

        assertThrows(
                UnprocessableEntityException.class,
                () -> service.rollbackRelease(1L, new RollbackReleaseRequest(2L, "bad lineage"))
        );

        verify(releaseBatchRepository, never()).save(org.mockito.ArgumentMatchers.any(ReleaseBatch.class));
        verify(changeRequestRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(outboxEventPublisher, never()).publish(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyMap()
        );
    }
}
