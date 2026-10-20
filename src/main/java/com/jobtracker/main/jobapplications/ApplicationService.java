package com.jobtracker.main.jobapplications;
import com.jobtracker.main.users.UserRepository;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    private static final char ESCAPE = '\\';

    static String escapeLike(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
    public static Predicate getFilter(Root<Application> root, CriteriaQuery<?> query, CriteriaBuilder builder,
                        UUID userId, ApplicationStatus status, String company, String position) {
        CriteriaQuery<Application> typedQuery = (CriteriaQuery<Application>) query;
        Predicate predicate = builder.conjunction();
        if(query.getResultType() != Long.class && query.getResultType() != long.class) {
            root.fetch("statusChanges", JoinType.LEFT);
            typedQuery.select(root);
        }
        predicate = builder.and(predicate, builder.equal(root.get("user").get("id"), userId));
        if(status != null) {
            predicate = builder.and(predicate, builder.equal(
                root.get("currentStatus"), status
            ));
        }

        if(company != null) {
            predicate = builder.and(predicate, builder.like(
                builder.lower(root.get("company")), "%" + escapeLike(company.toLowerCase()) + "%", ESCAPE
            ));
        }

        if(position != null) {
            predicate = builder.and(predicate, builder.like(
                builder.lower(root.get("position")), "%" + escapeLike(position.toLowerCase()) + "%", ESCAPE
            ));
        }
        return predicate;
    }
    public Page<ApplicationResponseDto> getApplications(UUID userId, Pageable page, ApplicationStatus status, String company, String position) {
        return applicationRepository.findAll((root, query, builder) ->
                getFilter(root, query, builder, userId, status, company, position), page).map(this::getResponse);
    }
    public Optional<ApplicationResponseDto> getApplicationById(UUID id, UUID userId) {
        return applicationRepository.getApplicationByIdAndOwner(id, userId).map(this::getResponse);
    }
    @Transactional
    public Optional<ApplicationResponseDto> putApplication(UUID id, UpdateApplicationDto applicationDto, UUID userId) {
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