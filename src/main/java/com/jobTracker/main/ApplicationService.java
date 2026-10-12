package com.jobtracker.main;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    public ApplicationService(ApplicationRepository applicationRepository, UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    private ApplicationResponseDto getResponse(Application application) {
        List<ChangeStatusResponseDto> statusChanges = application.getStatusChanges().stream()
                .map(statusChange -> new ChangeStatusResponseDto(statusChange.getStatus(),
                        statusChange.getCause(), statusChange.getDetail(), statusChange.getCreatedAt())).toList();
        return new ApplicationResponseDto(application.getApplicationId(), application.getCompany(), application.getPosition(),
                application.getNotes(), application.getLinks(), statusChanges);
    }

    @Transactional
    public ApplicationResponseDto saveApplication(ApplicationDto applicationDto, UUID userId) {
        Application application = new Application(applicationDto.company(), applicationDto.position(), applicationDto.status(),
                applicationDto.notes(), applicationDto.links(), userRepository.getReferenceById(userId));
        application = applicationRepository.save(application);
        return getResponse(application);
    }
    public List<ApplicationResponseDto> getApplications(UUID userId) {
        return applicationRepository.getApplicationsByOwner(userId).stream().map(this::getResponse).toList();
    }
    public Optional<ApplicationResponseDto> getApplicationById(UUID id, UUID userId) {
        return applicationRepository.getApplicationByIdAndOwner(id, userId).map(this::getResponse);
    }
    @Transactional
    public Optional<ApplicationResponseDto> putApplication(UUID id, ApplicationDto applicationDto, UUID userId) {
        return applicationRepository.getApplicationByIdAndOwner(id, userId)
                .map(application ->
                    getResponse(
                            application.updateApplication(
                                    applicationDto.company(), applicationDto.position(), applicationDto.notes(), applicationDto.links()
                            )
                    )
                );
    }
    @Transactional()
    public boolean deleteApplication(UUID id, UUID userId) {
        Optional<Application> previousApplication = applicationRepository.getApplicationByIdAndOwner(id, userId);
        previousApplication.ifPresent(applicationRepository::delete);
        return previousApplication.isPresent();
    }
    @Transactional
    public Optional<ApplicationResponseDto> changeStatus(UUID applicationId, ChangeStatusDto statusDto, UUID userId) {
        return applicationRepository.getLockedApplicationByIdAndOwner(applicationId, userId)
                .map(application -> {
                    application.changeStatus(statusDto.status());
                    return getResponse(application);
                });
    }
}
