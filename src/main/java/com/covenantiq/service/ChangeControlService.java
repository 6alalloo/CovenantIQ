package com.covenantiq.service;

import com.covenantiq.domain.ChangeRequest;
import com.covenantiq.domain.ChangeRequestItem;
import com.covenantiq.domain.ReleaseAudit;
import com.covenantiq.domain.ReleaseBatch;
import com.covenantiq.dto.request.CreateChangeRequestRequest;
import com.covenantiq.dto.request.CreateReleaseRequest;
import com.covenantiq.dto.request.RollbackReleaseRequest;
import com.covenantiq.dto.response.ChangeRequestItemResponse;
import com.covenantiq.dto.response.ChangeRequestResponse;
import com.covenantiq.dto.response.ReleaseAuditResponse;
import com.covenantiq.dto.response.ReleaseBatchResponse;
import com.covenantiq.enums.ChangeRequestStatus;
import com.covenantiq.exception.ResourceNotFoundException;
import com.covenantiq.exception.UnprocessableEntityException;
import com.covenantiq.repository.ChangeRequestItemRepository;
import com.covenantiq.repository.ChangeRequestRepository;
import com.covenantiq.repository.ReleaseAuditRepository;
import com.covenantiq.repository.ReleaseBatchRepository;
import com.covenantiq.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ChangeControlService {

    private final ChangeRequestRepository changeRequestRepository;
    private final ChangeRequestItemRepository changeRequestItemRepository;
    private final ReleaseBatchRepository releaseBatchRepository;
    private final ReleaseAuditRepository releaseAuditRepository;
    private final CurrentUserService currentUserService;
    private final OutboxEventPublisher outboxEventPublisher;

    public ChangeControlService(
            ChangeRequestRepository changeRequestRepository,
            ChangeRequestItemRepository changeRequestItemRepository,
            ReleaseBatchRepository releaseBatchRepository,
            ReleaseAuditRepository releaseAuditRepository,
            CurrentUserService currentUserService,
            OutboxEventPublisher outboxEventPublisher
    ) {
        this.changeRequestRepository = changeRequestRepository;
        this.changeRequestItemRepository = changeRequestItemRepository;
        this.releaseBatchRepository = releaseBatchRepository;
        this.releaseAuditRepository = releaseAuditRepository;
        this.currentUserService = currentUserService;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Transactional
    public ChangeRequestResponse createChangeRequest(CreateChangeRequestRequest request) {
        ChangeRequest changeRequest = new ChangeRequest();
        changeRequest.setType(request.type());
        changeRequest.setStatus(ChangeRequestStatus.SUBMITTED);
        changeRequest.setRequestedBy(currentUserService.usernameOrSystem());
        changeRequest.setJustification(request.justification().trim());
        ChangeRequest saved = changeRequestRepository.save(changeRequest);

        for (CreateChangeRequestRequest.ChangeRequestItemInput itemInput : request.items()) {
            ChangeRequestItem item = new ChangeRequestItem();
            item.setChangeRequestId(saved.getId());
            item.setArtifactType(itemInput.artifactType().trim().toUpperCase());
            item.setArtifactId(itemInput.artifactId());
            item.setFromVersion(itemInput.fromVersion());
            item.setToVersion(itemInput.toVersion());
            item.setDiffJson(itemInput.diffJson());
            changeRequestItemRepository.save(item);
        }
        return toChangeRequestResponse(saved);
    }

    @Transactional
    public ChangeRequestResponse approveChangeRequest(Long id) {
        ChangeRequest changeRequest = changeRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Change request " + id + " not found"));
        changeRequest.setStatus(ChangeRequestStatus.APPROVED);
        changeRequest.setApprovedBy(currentUserService.usernameOrSystem());
        changeRequest.setApprovedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return toChangeRequestResponse(changeRequestRepository.save(changeRequest));
    }

    @Transactional
    public ReleaseBatchResponse createRelease(CreateReleaseRequest request) {
        ChangeRequest changeRequest = changeRequestRepository.findById(request.changeRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Change request " + request.changeRequestId() + " not found"));
        if (changeRequest.getStatus() != ChangeRequestStatus.APPROVED) {
            throw new UnprocessableEntityException("Only APPROVED change requests can be released");
        }
        ReleaseBatch releaseBatch = new ReleaseBatch();
        releaseBatch.setChangeRequestId(changeRequest.getId());
        releaseBatch.setReleaseTag(request.releaseTag().trim());
        releaseBatch.setReleasedBy(currentUserService.usernameOrSystem());
        ReleaseBatch saved = releaseBatchRepository.save(releaseBatch);

        changeRequest.setStatus(ChangeRequestStatus.RELEASED);
        changeRequestRepository.save(changeRequest);

        audit(saved.getId(), "RELEASE_CREATED", Map.of(
                "changeRequestId", changeRequest.getId(),
                "releaseTag", saved.getReleaseTag()
        ));
        outboxEventPublisher.publish("ReleaseBatch", saved.getId(), "ReleaseCreated", Map.of(
                "releaseBatchId", saved.getId(),
                "changeRequestId", saved.getChangeRequestId(),
                "releaseTag", saved.getReleaseTag()
        ));
        return toReleaseResponse(saved);
    }

    @Transactional
    public ReleaseBatchResponse rollbackRelease(Long releaseId, RollbackReleaseRequest request) {
        ReleaseBatch sourceRelease = releaseBatchRepository.findById(releaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Release " + releaseId + " not found"));
        ReleaseBatch rollbackTarget = releaseBatchRepository.findById(request.targetReleaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Target release " + request.targetReleaseId() + " not found"));
        if (!Objects.equals(sourceRelease.getChangeRequestId(), rollbackTarget.getChangeRequestId())) {
            throw new UnprocessableEntityException(
                    "Rollback target must belong to the same change request lineage"
            );
        }

        ReleaseBatch rollback = new ReleaseBatch();
        rollback.setChangeRequestId(sourceRelease.getChangeRequestId());
        rollback.setReleaseTag(sourceRelease.getReleaseTag() + "-rollback");
        rollback.setReleasedBy(currentUserService.usernameOrSystem());
        rollback.setRollbackOfReleaseId(request.targetReleaseId());
        ReleaseBatch saved = releaseBatchRepository.save(rollback);

        ChangeRequest changeRequest = changeRequestRepository.findById(sourceRelease.getChangeRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Change request " + sourceRelease.getChangeRequestId() + " not found"));
        changeRequest.setStatus(ChangeRequestStatus.ROLLED_BACK);
        changeRequestRepository.save(changeRequest);

        audit(saved.getId(), "RELEASE_ROLLED_BACK", Map.of(
                "releaseId", releaseId,
                "targetReleaseId", request.targetReleaseId(),
                "justification", request.justification()
        ));
        outboxEventPublisher.publish("ReleaseBatch", saved.getId(), "ReleaseRolledBack", Map.of(
                "releaseBatchId", saved.getId(),
                "rollbackOfReleaseId", request.targetReleaseId(),
                "justification", request.justification()
        ));
        return toReleaseResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ChangeRequestResponse> listChangeRequests() {
        return changeRequestRepository.findAll().stream().map(this::toChangeRequestResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReleaseBatchResponse> listReleases() {
        return releaseBatchRepository.findAll().stream().map(this::toReleaseResponse).toList();
    }

    private void audit(Long releaseBatchId, String action, Map<String, Object> details) {
        ReleaseAudit audit = new ReleaseAudit();
        audit.setReleaseBatchId(releaseBatchId);
        audit.setAction(action);
        audit.setActor(currentUserService.usernameOrSystem());
        audit.setDetailsJson(writeJson(details));
        releaseAuditRepository.save(audit);
    }

    private ChangeRequestResponse toChangeRequestResponse(ChangeRequest changeRequest) {
        List<ChangeRequestItemResponse> items = changeRequestItemRepository.findByChangeRequestId(changeRequest.getId())
                .stream()
                .map(i -> new ChangeRequestItemResponse(
                        i.getId(),
                        i.getArtifactType(),
                        i.getArtifactId(),
                        i.getFromVersion(),
                        i.getToVersion(),
                        i.getDiffJson()
                ))
                .toList();
        return new ChangeRequestResponse(
                changeRequest.getId(),
                changeRequest.getType(),
                changeRequest.getStatus(),
                changeRequest.getRequestedBy(),
                changeRequest.getRequestedAt(),
                changeRequest.getApprovedBy(),
                changeRequest.getApprovedAt(),
                changeRequest.getJustification(),
                items
        );
    }

    private ReleaseBatchResponse toReleaseResponse(ReleaseBatch releaseBatch) {
        List<ReleaseAuditResponse> audits = releaseAuditRepository.findByReleaseBatchIdOrderByTimestampUtcDesc(releaseBatch.getId())
                .stream()
                .map(a -> new ReleaseAuditResponse(
                        a.getId(),
                        a.getAction(),
                        a.getActor(),
                        a.getDetailsJson(),
                        a.getTimestampUtc()
                ))
                .toList();
        return new ReleaseBatchResponse(
                releaseBatch.getId(),
                releaseBatch.getChangeRequestId(),
                releaseBatch.getReleaseTag(),
                releaseBatch.getReleasedBy(),
                releaseBatch.getReleasedAt(),
                releaseBatch.getRollbackOfReleaseId(),
                audits
        );
    }

    private String writeJson(Object details) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(details);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
