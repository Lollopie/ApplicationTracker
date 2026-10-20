package com.jobtracker.main;

import com.jobtracker.main.jobapplications.*;
import com.jobtracker.main.users.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class JobApplicationStatusUnitTest {
    Map<JobApplicationStatus, List<JobApplicationStatus>> allowedStatusTransitions = Map.of(
        JobApplicationStatus.SAVED, List.of(JobApplicationStatus.APPLIED, JobApplicationStatus.REJECTED),
        JobApplicationStatus.APPLIED, List.of(JobApplicationStatus.INTERVIEW, JobApplicationStatus.REJECTED),
        JobApplicationStatus.INTERVIEW, List.of(JobApplicationStatus.INTERVIEW, JobApplicationStatus.OFFER, JobApplicationStatus.REJECTED),
        JobApplicationStatus.OFFER, List.of(JobApplicationStatus.REJECTED),
        JobApplicationStatus.REJECTED, List.of()
    );


    @ParameterizedTest
    @MethodSource("applicationStatusProvider")
    void testApplicationTransition(JobApplicationStatus currentStatus, JobApplicationStatus newStatus) {
        assertEquals(currentStatus.allowedTransitionSet().contains(newStatus), allowedStatusTransitions.get(currentStatus).contains(newStatus));
    }
    static Stream<Arguments> applicationStatusProvider() {
        Stream.Builder<Arguments> argumentBuilder = Stream.builder();
        for (JobApplicationStatus jobApplicationStatus1 : JobApplicationStatus.values()) {
            for (JobApplicationStatus jobApplicationStatus2 : JobApplicationStatus.values()) {
                argumentBuilder.add(Arguments.of(jobApplicationStatus1, jobApplicationStatus2));
            }
        }
        return argumentBuilder.build();
    }

    @Test
    public void testApplicationUsesPassedStatus() {
        User user = new User("A", "B");
        JobApplication jobApplication = new JobApplication("X", "Y", JobApplicationStatus.APPLIED, "", "", user);
        List<StatusChange> statusChanges = jobApplication.getStatusChanges();
        assertNotNull(statusChanges);
        assertFalse(statusChanges.isEmpty());
        assertEquals(JobApplicationStatus.APPLIED, statusChanges.getFirst().getStatus());
        assertEquals(1, statusChanges.size());
        assertEquals(StatusChangeCause.MANUAL, statusChanges.getFirst().getCause());
    }
    @Test
    public void testApplicationImmediateStatusChange() {
        User user = new User("A", "B");
        JobApplication jobApplication = new JobApplication("X", "Y", JobApplicationStatus.SAVED, "", "", user);
        jobApplication.changeStatus(JobApplicationStatus.APPLIED);
        assertEquals(JobApplicationStatus.APPLIED, jobApplication.getStatusChanges().getLast().getStatus());
    }
}
