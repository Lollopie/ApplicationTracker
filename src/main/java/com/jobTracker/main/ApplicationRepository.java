package com.jobtracker.main;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
}
