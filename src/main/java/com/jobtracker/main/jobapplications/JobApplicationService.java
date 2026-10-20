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
public class JobApplicationService {
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;
    public JobApplicationService(JobApplicationRepository jobApplicationRepository, UserRepository userRepository) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.userRepository = userRepository;
    }

    private JobApplicationResponseDto getResponse(JobApplication jobApplication) {
        List<ChangeStatusResponseDto> statusChanges = jobApplication.getStatusChanges().stream()
                .map(statusChange -> new ChangeStatusResponseDto(statusChange.getStatus(),
                        statusChange.getCause(), statusChange.getDetail(), statusChange.getCreatedAt())).toList();
        return new JobApplicationResponseDto(jobApplication.getApplicationId(), jobApplication.getCompany(), jobApplication.getPosition(),
                jobApplication.getNotes(), jobApplication.getLinks(), statusChanges);
    }

    @Transactional
    public JobApplicationResponseDto saveApplication(JobApplicationDto jobApplicationDto, UUID userId) {
        JobApplication jobApplication = new JobApplication(jobApplicationDto.company(), jobApplicationDto.position(), jobApplicationDto.status(),
                jobApplicationDto.notes(), jobApplicationDto.links(), userRepository.getReferenceById(userId));
        jobApplication = jobApplicationRepository.save(jobApplication);
        return getResponse(jobApplication);
    }

    private static final char ESCAPE = '\\';

    static String escapeLike(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
    public static Predicate getFilter(Root<JobApplication> root, CriteriaQuery<?> query, CriteriaBuilder builder,
                                      UUID userId, JobApplicationStatus status, String company, String position) {
        CriteriaQuery<JobApplication> typedQuery = (CriteriaQuery<JobApplication>) query;
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
    public Page<JobApplicationResponseDto> getApplications(UUID userId, Pageable page, JobApplicationStatus status, String company, String position) {
        return jobApplicationRepository.findAll((root, query, builder) ->
                getFilter(root, query, builder, userId, status, company, position), page).map(this::getResponse);
    }
    public Optional<JobApplicationResponseDto> getApplicationById(UUID id, UUID userId) {
        return jobApplicationRepository.getApplicationByIdAndOwner(id, userId).map(this::getResponse);
    }
    @Transactional
    public Optional<JobApplicationResponseDto> putApplication(UUID id, UpdateJobApplicationDto applicationDto, UUID userId) {
        return jobApplicationRepository.getApplicationByIdAndOwner(id, userId)
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
        Optional<JobApplication> previousApplication = jobApplicationRepository.getApplicationByIdAndOwner(id, userId);
        previousApplication.ifPresent(jobApplicationRepository::delete);
        return previousApplication.isPresent();
    }
    @Transactional
    public Optional<JobApplicationResponseDto> changeStatus(UUID applicationId, ChangeStatusDto statusDto, UUID userId) {
        return jobApplicationRepository.getLockedApplicationByIdAndOwner(applicationId, userId)
                .map(application -> {
                    application.changeStatus(statusDto.status());
                    return getResponse(application);
                });
    }
}