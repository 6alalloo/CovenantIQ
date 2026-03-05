package com.covenantiq.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "release_batch")
public class ReleaseBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long changeRequestId;

    @Column(nullable = false, length = 120)
    private String releaseTag;

    @Column(nullable = false)
    private String releasedBy;

    @Column(nullable = false)
    private OffsetDateTime releasedAt;

    private Long rollbackOfReleaseId;

    @PrePersist
    void prePersist() {
        releasedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public Long getChangeRequestId() {
        return changeRequestId;
    }

    public void setChangeRequestId(Long changeRequestId) {
        this.changeRequestId = changeRequestId;
    }

    public String getReleaseTag() {
        return releaseTag;
    }

    public void setReleaseTag(String releaseTag) {
        this.releaseTag = releaseTag;
    }

    public String getReleasedBy() {
        return releasedBy;
    }

    public void setReleasedBy(String releasedBy) {
        this.releasedBy = releasedBy;
    }

    public OffsetDateTime getReleasedAt() {
        return releasedAt;
    }

    public Long getRollbackOfReleaseId() {
        return rollbackOfReleaseId;
    }

    public void setRollbackOfReleaseId(Long rollbackOfReleaseId) {
        this.rollbackOfReleaseId = rollbackOfReleaseId;
    }
}
