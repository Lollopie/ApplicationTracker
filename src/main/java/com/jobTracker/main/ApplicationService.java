package com.jobtracker.main;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    public ApplicationService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    private ApplicationResponseDto getResponse(Application application) {
        return new ApplicationResponseDto(application.getApplicationId(), application.getCompany(), application.getPosition(),
                application.getStatus(), application.getNotes(), application.getLinks());
    }

    public ApplicationResponseDto saveApplication(ApplicationDto applicationDto) {
        Application application = new Application(applicationDto.company(), applicationDto.position(), applicationDto.status(),
                applicationDto.notes(), applicationDto.links());
        application = applicationRepository.save(application);
        return getResponse(application);
    }
    public List<ApplicationResponseDto> getApplications() {
        return applicationRepository.findAll().stream().map(this::getResponse).toList();
    }
    public Optional<ApplicationResponseDto> getApplicationById(UUID id) {
        return applicationRepository.findById(id).map(this::getResponse);
    }
    @Transactional
    public Optional<ApplicationResponseDto> putApplication(UUID id, ApplicationDto applicationDto) {
        return applicationRepository.findById(id)
                .map(application ->
                    getResponse(
                            application.updateApplication(
                                    applicationDto.company(), applicationDto.position(), applicationDto.status(),
                                    applicationDto.notes(), applicationDto.links()
                            )
                    )
                );
    }
    @Transactional
    public boolean deleteApplication(UUID id) {
        Optional<Application> previousApplication = applicationRepository.findById(id);
        previousApplication.ifPresent(applicationRepository::delete);
        return previousApplication.isPresent();
    }
}
