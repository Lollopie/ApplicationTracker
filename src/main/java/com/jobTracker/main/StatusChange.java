package com.jobtracker.main;

import jakarta.persistence.*;
import org.hibernate.generator.EventType;

import org.hibernate.annotations.Generated;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="status_change")
public class StatusChange {
    protected StatusChange() {}

    public StatusChange(Application application, ApplicationStatus status) {
        this.application = application;
        this.status = status;
        this.cause = StatusChangeCause.MANUAL;
    }



    public StatusChange(Application application, ApplicationStatus status, StatusChangeCause cause) {
        this.application = application;
        this.status = status;
        this.cause = cause;
    }

    public StatusChange(Application application, ApplicationStatus status, StatusChangeCause cause, String detail) {
        this.application = application;
        this.status = status;
        this.cause = cause;
        this.detail = detail;
    }

    @Id
    @Column(name="id", updatable = false, nullable = false)
    @Generated(event = EventType.INSERT)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="application_id")
    private Application application;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusChangeCause cause;

    private String detail;

    @Column(name="created_at", updatable = false)
    @Generated(event = EventType.INSERT)
    private Instant createdAt;

    public ApplicationStatus getStatus() {
        return status;
    }

    public StatusChangeCause getCause() {
        return cause;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
