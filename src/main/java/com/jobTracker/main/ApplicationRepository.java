package com.jobtracker.main;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    @Query("SELECT a FROM Application a LEFT JOIN FETCH a.statusChanges WHERE a.user.id = ?1")
    List<Application> getApplicationsByOwner(UUID userId);
    @Query("SELECT a FROM Application a LEFT JOIN FETCH a.statusChanges WHERE a.applicationId = ?1 AND a.user.id = ?2")
    Optional<Application> getApplicationByIdAndOwner(UUID id, UUID userId);
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT a FROM Application a WHERE a.applicationId = ?1 AND a.user.id = ?2")
    Optional<Application> getLockedApplicationByIdAndOwner(UUID id, UUID userId);
}
