package com.jobtracker.main;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    public ApplicationService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }
    public Application saveApplication(ApplicationDto applicationDto) {
        Application application = new Application(applicationDto.company(), applicationDto.position(), applicationDto.status(),
                applicationDto.notes(), applicationDto.links(), UUID.randomUUID());
        applicationRepository.saveApplication(application);
        return application;
    }
    public List<Application> getApplications() {
        return applicationRepository.getApplications();
    }
    public Optional<Application> getApplicationById(UUID id) {
        return applicationRepository.getApplicationById(id);
    }
    public Optional<Application> putApplication(UUID id, ApplicationDto applicationDto) {
        Application application = new Application(applicationDto.company(), applicationDto.position(), applicationDto.status(),
                applicationDto.notes(), applicationDto.links(), id);
        return applicationRepository.putApplication(id, application);
    }
    public Optional<Application> deleteApplication(UUID id) {
        return applicationRepository.deleteApplication(id);
    }
}
