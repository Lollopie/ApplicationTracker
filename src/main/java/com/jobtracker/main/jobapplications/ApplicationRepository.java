package com.jobtracker.main.jobapplications;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {
    @Query("SELECT a FROM Application a LEFT JOIN FETCH a.statusChanges WHERE a.applicationId = ?1 AND a.user.id = ?2")
    Optional<Application> getApplicationByIdAndOwner(UUID id, UUID userId);
    @Query("SELECT a FROM Application a WHERE a.applicationId = ?1 AND a.user.id = ?2")
    Optional<Application> getLockedApplicationByIdAndOwner(UUID id, UUID userId);
    @NullMarked
    Page<Application> findAll(Specification<Application> spec, Pageable pageable);
}
