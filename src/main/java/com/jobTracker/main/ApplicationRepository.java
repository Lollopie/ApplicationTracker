package com.jobtracker.main;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    @Query("SELECT a FROM Application a LEFT JOIN FETCH a.statusChanges")
    List<Application> getApplications();
    @Query("SELECT a FROM Application a LEFT JOIN FETCH a.statusChanges where a.applicationId = ?1")
    Optional<Application> getApplicationById(UUID id);
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT a FROM Application a where a.applicationId = ?1")
    Optional<Application> getLockedApplicationById(UUID id);
}
