package com.jobtracker.main.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "users")
public class User {

    protected User() {}

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @Generated(event = EventType.INSERT)
    private UUID id;
    @Column(nullable = false, unique = true, columnDefinition = "citext")
    private String username;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "created_at", updatable = false, nullable = false)
    @Generated(event = EventType.INSERT)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
