package com.jobtracker.main;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

@Repository
public class ApplicationRepository {
    private final ConcurrentHashMap<UUID, Application> applications = new ConcurrentHashMap<>();

    public void saveApplication(Application application) {
        applications.put(application.id(), application);
    }

    public List<Application> getApplications() {
        return applications.values().stream().toList();
    }

    public Optional<Application> getApplicationById(UUID id) {
        return Optional.ofNullable(applications.get(id));
    }
    public Optional<Application> putApplication(UUID id, Application application) {
        return Optional.ofNullable(applications.put(id, application));
    }
    public Optional<Application> deleteApplication(UUID id) {
        return Optional.ofNullable(applications.remove(id));
    }
}
