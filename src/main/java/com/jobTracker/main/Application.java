package com.jobtracker.main;

import jakarta.persistence.*;
import org.hibernate.generator.EventType;

import org.hibernate.annotations.Generated;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name="application")
public class Application {

    protected Application() {}

    public Application(String company, String position, ApplicationStatus status, String notes, String links) {
        this.company = company;
        this.position = position;
        this.notes = notes;
        this.links = links;
        this.statusChanges = new ArrayList<>();
        this.statusChanges.add(new StatusChange(this, status));
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


    @Version
    private int version;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<StatusChange> statusChanges;

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

    public List<StatusChange> getStatusChanges() {
        return List.copyOf(statusChanges);
    }

    public Application updateApplication(String company, String position, String notes, String links) {
        this.company = company;
        this.position = position;
        this.notes = notes;
        this.links = links;
        return this;
    }
    public StatusChange changeStatus(ApplicationStatus status) {
        return this.changeStatus(status, StatusChangeCause.MANUAL);
    }
    public StatusChange changeStatus(ApplicationStatus status, StatusChangeCause cause) {
        return this.changeStatus(status, cause, null);
    }
    public StatusChange changeStatus(ApplicationStatus status, StatusChangeCause cause, String detail) {
        ApplicationStatus currentStatus = this.statusChanges.getLast().getStatus();
        if(!currentStatus.allowedTransitionSet().contains(status)) {
            throw new IllegalStatusTransitionException("Cannot change from status " + currentStatus + " to status " + status);
        }
        StatusChange statusChange = new StatusChange(this, status, cause, detail);
        this.statusChanges.add(statusChange);
        return statusChange;
    }
}
