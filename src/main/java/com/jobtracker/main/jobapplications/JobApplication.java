package com.jobtracker.main.jobapplications;

import com.jobtracker.main.IllegalStatusTransitionException;
import com.jobtracker.main.users.User;
import jakarta.persistence.*;
import org.hibernate.generator.EventType;

import org.hibernate.annotations.Generated;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name="application")
public class JobApplication {

    protected JobApplication() {}

    public JobApplication(String company, String position, JobApplicationStatus status, String notes, String links, User user) {
        this.company = company;
        this.position = position;
        this.notes = notes;
        this.links = links;
        this.statusChanges = new ArrayList<>();
        this.statusChanges.add(new StatusChange(this, status));
        this.user = user;
        this.currentStatus = status;
    }

    @Id
    @Column(name="application_id", updatable = false, nullable = false)
    @Generated(event = EventType.INSERT)
    private UUID applicationId;
    @Column(nullable = false)
    private String company;
    @Column(nullable = false)
    private String position;

    private String notes;

    private String links;

    @Column(name="current_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private JobApplicationStatus currentStatus;

    @Version
    private int version;

    @OneToMany(mappedBy = "application", cascade = CascadeType.PERSIST)
    @OrderBy("id ASC")
    private List<StatusChange> statusChanges;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getCompany() {
        return company;
    }

    public String getPosition() {
        return position;
    }

    public String getNotes() {
        return notes;
    }

    public String getLinks() {
        return links;
    }

    public User getUser() {
        return user;
    }

    public List<StatusChange> getStatusChanges() {
        return List.copyOf(statusChanges);
    }

    public JobApplicationStatus getCurrentStatus() {
        return currentStatus;
    }

    public JobApplication updateApplication(String company, String position, String notes, String links) {
        this.company = company;
        this.position = position;
        this.notes = notes;
        this.links = links;
        return this;
    }
    public StatusChange changeStatus(JobApplicationStatus status) {
        return this.changeStatus(status, StatusChangeCause.MANUAL);
    }
    public StatusChange changeStatus(JobApplicationStatus status, StatusChangeCause cause) {
        return this.changeStatus(status, cause, null);
    }
    public StatusChange changeStatus(JobApplicationStatus status, StatusChangeCause cause, String detail) {
        if(!this.currentStatus.allowedTransitionSet().contains(status)) {
            throw new IllegalStatusTransitionException("Cannot change from status " + this.currentStatus + " to status " + status);
        }
        StatusChange statusChange = new StatusChange(this, status, cause, detail);
        this.statusChanges.add(statusChange);
        this.currentStatus = statusChange.getStatus();
        return statusChange;
    }
}
