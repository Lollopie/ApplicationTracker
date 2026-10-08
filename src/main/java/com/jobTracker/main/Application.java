package com.jobtracker.main;

import jakarta.persistence.*;
import org.hibernate.generator.EventType;

import org.hibernate.annotations.Generated;
import java.util.UUID;

@Entity
@Table(name="application")
public class Application {
    protected Application() {}

    public Application(String company, String position, ApplicationStatus status, String notes, String links) {
        this.company = company;
        this.position = position;
        this.status = status;
        this.notes = notes;
        this.links = links;
    }

    @Id
    @Column(name="application_id", updatable = false, nullable = false, columnDefinition = "UUID DEFAULT uuidv7()")
    @Generated(event = EventType.INSERT)
    private UUID applicationId;
    @Column(nullable = false)
    private String company;
    @Column(nullable = false)
    private String position;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private String notes;

    private String links;

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getCompany() {
        return company;
    }

    public String getPosition() {
        return position;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public String getLinks() {
        return links;
    }

    public Application updateApplication(String company, String position, ApplicationStatus status, String notes, String links) {
        this.company = company;
        this.position = position;
        this.status = status;
        this.notes = notes;
        this.links = links;
        return this;
    }
}
