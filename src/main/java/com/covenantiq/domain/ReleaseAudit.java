package com.covenantiq.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "release_audit")
public class ReleaseAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long releaseBatchId;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(nullable = false)
    private String actor;

    @Lob
    @Column(nullable = false)
    private String detailsJson;

    @Column(nullable = false)
    private OffsetDateTime timestampUtc;

    @PrePersist
    void prePersist() {
        timestampUtc = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public Long getReleaseBatchId() {
        return releaseBatchId;
    }

    public void setReleaseBatchId(Long releaseBatchId) {
        this.releaseBatchId = releaseBatchId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }

    public OffsetDateTime getTimestampUtc() {
        return timestampUtc;
    }
}
