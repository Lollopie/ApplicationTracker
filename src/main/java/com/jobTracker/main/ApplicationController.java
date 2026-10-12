package com.jobtracker.main;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/applications")
public class ApplicationController  {
    private ResponseStatusException createResponseStatusException(){
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Application Not Found");
    }
    private final ApplicationService applicationService;
    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }
    @PostMapping
    public ResponseEntity<ApplicationResponseDto> createApplication(@Valid @RequestBody ApplicationDto applicationDto,
                                                                    @AuthenticationPrincipal UserDetailModel userDetails) {
        ApplicationResponseDto application = applicationService.saveApplication(applicationDto, userDetails.getId());
        return ResponseEntity.created(URI.create("/applications/" + application.id())).body(application);
    }
    @GetMapping
    public List<ApplicationResponseDto> listApplications(@AuthenticationPrincipal UserDetailModel userDetails) {
        return applicationService.getApplications(userDetails.getId());
    }
    @GetMapping("{id}")
    public ResponseEntity<ApplicationResponseDto> getApplicationById(@PathVariable UUID id, @AuthenticationPrincipal UserDetailModel userDetails) {
        return applicationService.getApplicationById(id, userDetails.getId()).map(ResponseEntity::ok).orElseThrow(this::createResponseStatusException);
    }
    @PutMapping("{id}")
    public ResponseEntity<ApplicationResponseDto> putApplication(@PathVariable UUID id, @Valid @RequestBody ApplicationDto applicationDto,
                                                                 @AuthenticationPrincipal UserDetailModel userDetails) {
        return applicationService.putApplication(id, applicationDto, userDetails.getId()).map(ResponseEntity::ok).orElseThrow(this::createResponseStatusException);
    }
    @PostMapping("{id}/status")
    public ResponseEntity<ApplicationResponseDto> changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusDto changeStatusDto,
                                                               @AuthenticationPrincipal UserDetailModel userDetails) {
        return applicationService.changeStatus(id, changeStatusDto, userDetails.getId()).map(ResponseEntity::ok).orElseThrow(this::createResponseStatusException);
    }
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable UUID id, @AuthenticationPrincipal UserDetailModel userDetails) {
        boolean success = applicationService.deleteApplication(id, userDetails.getId());
        if (success) {
            return ResponseEntity.noContent().build();
        }
        throw createResponseStatusException();
    }
}
