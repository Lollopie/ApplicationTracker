package com.jobTracker.main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/applications")
public class ApplicationController  {
    private final ApplicationService applicationService;
    @Autowired
    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }
    @PostMapping
    public ResponseEntity<Application> createApplication(@RequestBody ApplicationDto applicationDto) throws URISyntaxException {
        Application application = new Application(applicationDto.company(), applicationDto.position(), applicationDto.status(),
                applicationDto.notes(), applicationDto.links(), UUID.randomUUID());
        applicationService.saveApplication(application);
        return ResponseEntity.created(new URI("/applications/" + application.id())).body(application);
    }
    @GetMapping
    public List<Application> listApplications() {
        return applicationService.getApplications();
    }
    @GetMapping("{id}")
    public ResponseEntity<Application> getApplicationById(@PathVariable UUID id) {
        Optional<Application> applicationOptional = applicationService.getApplicationById(id);
        if (applicationOptional.isPresent()) {
            return ResponseEntity.ok().body(applicationOptional.get());
        }
        return ResponseEntity.notFound().build();
    }
    @PutMapping("{id}")
    public ResponseEntity<Application> putApplication(@PathVariable UUID id, @RequestBody ApplicationDto applicationDto) throws URISyntaxException {
        Application application = new Application(applicationDto.company(), applicationDto.position(), applicationDto.status(),
                applicationDto.notes(), applicationDto.links(), id);
        Optional<Application> previousApplication = applicationService.putApplication(id, application);
        if (previousApplication.isPresent()) {
            return ResponseEntity.ok().body(application);
        }
        return ResponseEntity.created(new URI("/applications/" + id)).body(application);
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
