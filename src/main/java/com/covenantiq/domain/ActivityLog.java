package com.covenantiq.domain;

import com.covenantiq.enums.ActivityEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityEventType eventType;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column
    private String username;

    @Column(nullable = false)
    private OffsetDateTime timestampUtc;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column
    private Long loanId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ActivityEventType getEventType() {
        return eventType;
    }

    public void setEventType(ActivityEventType eventType) {
        this.eventType = eventType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public OffsetDateTime getTimestampUtc() {
        return timestampUtc;
    }

    public void setTimestampUtc(OffsetDateTime timestampUtc) {
        this.timestampUtc = timestampUtc;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }
}
