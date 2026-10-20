package com.jobtracker.main.jobapplications;

import com.jobtracker.main.users.UserDetailModel;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/applications")
public class JobApplicationController {
    private ResponseStatusException createResponseStatusException(){
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Application Not Found");
    }
    private final JobApplicationService jobApplicationService;
    public JobApplicationController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }
    @PostMapping
    public ResponseEntity<JobApplicationResponseDto> createApplication(@Valid @RequestBody JobApplicationDto jobApplicationDto,
                                                                       @AuthenticationPrincipal UserDetailModel userDetails) {
        JobApplicationResponseDto application = jobApplicationService.saveApplication(jobApplicationDto, userDetails.getId());
        return ResponseEntity.created(URI.create("/applications/" + application.id())).body(application);
    }
    @GetMapping
    public Page<JobApplicationResponseDto> listApplications(@AuthenticationPrincipal UserDetailModel userDetails,
                                                            @PageableDefault(sort = "applicationId", direction = Sort.Direction.DESC)
                                                         Pageable pageable,
                                                            @RequestParam Map<String, String> allParams,
                                                            @RequestParam(required = false) JobApplicationStatus currentStatus,
                                                            @RequestParam(required = false) String company,
                                                            @RequestParam(required = false) String position

    ) {
        HashSet<String> allowedParameters = new HashSet<>(List.of("currentStatus", "company", "position", "sort", "page", "size"));
        if (!allowedParameters.containsAll(allParams.keySet())) {
            List<String> unknownParameters = allParams.keySet().stream().filter(param -> !allowedParameters.contains(param)).toList();
            Optional<String> outputString = unknownParameters.stream().reduce((output, current) -> output + ", " + current);
            outputString.map(string -> {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown parameters " + string);
            });
        }
        List<String> allowedSortOptions = List.of("applicationId", "company", "position", "currentStatus");
        if (!pageable.getSort().stream().allMatch(order -> allowedSortOptions.contains(order.getProperty()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Illegal Sort by field");
        }
        return jobApplicationService.getApplications(userDetails.getId(), pageable, currentStatus, company, position);
    }
    @GetMapping("{id}")
    public ResponseEntity<JobApplicationResponseDto> getApplicationById(@PathVariable UUID id, @AuthenticationPrincipal UserDetailModel userDetails) {
        return jobApplicationService.getApplicationById(id, userDetails.getId()).map(ResponseEntity::ok).orElseThrow(this::createResponseStatusException);
    }
    @PutMapping("{id}")
    public ResponseEntity<JobApplicationResponseDto> putApplication(@PathVariable UUID id, @Valid @RequestBody UpdateJobApplicationDto applicationDto,
                                                                    @AuthenticationPrincipal UserDetailModel userDetails) {
        return jobApplicationService.putApplication(id, applicationDto, userDetails.getId()).map(ResponseEntity::ok).orElseThrow(this::createResponseStatusException);
    }
    @PostMapping("{id}/status")
    public ResponseEntity<JobApplicationResponseDto> changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusDto changeStatusDto,
                                                                  @AuthenticationPrincipal UserDetailModel userDetails) {
        return jobApplicationService.changeStatus(id, changeStatusDto, userDetails.getId()).map(ResponseEntity::ok).orElseThrow(this::createResponseStatusException);
    }
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable UUID id, @AuthenticationPrincipal UserDetailModel userDetails) {
        boolean success = jobApplicationService.deleteApplication(id, userDetails.getId());
        if (success) {
            return ResponseEntity.noContent().build();
        }
        throw createResponseStatusException();
    }
}
