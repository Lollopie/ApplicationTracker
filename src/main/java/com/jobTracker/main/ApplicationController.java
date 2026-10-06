package com.jobtracker.main;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/applications")
public class ApplicationController  {

    private final ApplicationService applicationService;
    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }
    @PostMapping
    public ResponseEntity<Application> createApplication(@RequestBody ApplicationDto applicationDto) {
        Application application = applicationService.saveApplication(applicationDto);
        return ResponseEntity.created(URI.create("/applications/" + application.id())).body(application);
    }
    @GetMapping
    public List<Application> listApplications() {
        return applicationService.getApplications();
    }
    @GetMapping("{id}")
    public ResponseEntity<Application> getApplicationById(@PathVariable UUID id) {
        Optional<Application> applicationOptional = applicationService.getApplicationById(id);
        return ResponseEntity.of(applicationOptional);
    }
    @PutMapping("{id}")
    public ResponseEntity<Application> putApplication(@PathVariable UUID id, @RequestBody ApplicationDto applicationDto) {
        Optional<Application> application = applicationService.putApplication(id, applicationDto);
        return ResponseEntity.of(application);
    }
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable UUID id) {
        Optional<Application> deletedApplication = applicationService.deleteApplication(id);
        if (deletedApplication.isPresent()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
