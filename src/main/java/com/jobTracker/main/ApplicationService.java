package com.jobTracker.main;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    @Autowired
    public ApplicationService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }
    public void saveApplication(Application application) {
        applicationRepository.saveApplication(application);
    }
    public List<Application> getApplications() {
        return applicationRepository.getApplications();
    }
    public Optional<Application> getApplicationById(UUID id) {
        return applicationRepository.getApplicationById(id);
    }
    public Optional<Application> putApplication(UUID id, Application application) {
        return applicationRepository.putApplication(id, application);
    }
    public Optional<Application> deleteApplication(UUID id) {
        return applicationRepository.deleteApplication(id);
    }
}
