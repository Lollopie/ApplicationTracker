package com.jobtracker.main;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApplicationResponseDto> createApplication(@Valid @RequestBody ApplicationDto applicationDto) {
        ApplicationResponseDto application = applicationService.saveApplication(applicationDto);
        return ResponseEntity.created(URI.create("/applications/" + application.id())).body(application);
    }
    @GetMapping
    public List<ApplicationResponseDto> listApplications() {
        return applicationService.getApplications();
    }
    @GetMapping("{id}")
    public ResponseEntity<ApplicationResponseDto> getApplicationById(@PathVariable UUID id) {
        return applicationService.getApplicationById(id).map(ResponseEntity::ok).orElseThrow(this::createResponseStatusException);
    }
    @PutMapping("{id}")
    public ResponseEntity<ApplicationResponseDto> putApplication(@PathVariable UUID id, @Valid @RequestBody ApplicationDto applicationDto) {
        return applicationService.putApplication(id, applicationDto).map(ResponseEntity::ok).orElseThrow(this::createResponseStatusException);
    }
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable UUID id) {
        boolean success = applicationService.deleteApplication(id);
        if (success) {
            return ResponseEntity.noContent().build();
        }
        throw createResponseStatusException();
    }
}
