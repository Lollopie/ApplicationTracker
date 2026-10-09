package com.jobtracker.main;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicationStatusUnitTest {
    Map<ApplicationStatus, List<ApplicationStatus>> allowedStatusTransitions = Map.of(
        ApplicationStatus.SAVED, List.of(ApplicationStatus.APPLIED, ApplicationStatus.REJECTED),
        ApplicationStatus.APPLIED, List.of(ApplicationStatus.INTERVIEW, ApplicationStatus.REJECTED),
        ApplicationStatus.INTERVIEW, List.of(ApplicationStatus.INTERVIEW, ApplicationStatus.OFFER, ApplicationStatus.REJECTED),
        ApplicationStatus.OFFER, List.of(ApplicationStatus.REJECTED),
        ApplicationStatus.REJECTED, List.of()
    );


    @ParameterizedTest
    @MethodSource("applicationStatusProvider")
    void testApplicationTransition(ApplicationStatus currentStatus, ApplicationStatus newStatus) {
        assertEquals(currentStatus.allowedTransitionSet().contains(newStatus), allowedStatusTransitions.get(currentStatus).contains(newStatus));
    }
    static Stream<Arguments> applicationStatusProvider() {
        Stream.Builder<Arguments> argumentBuilder = Stream.builder();
        for (ApplicationStatus applicationStatus1 : ApplicationStatus.values()) {
            for (ApplicationStatus applicationStatus2 : ApplicationStatus.values()) {
                argumentBuilder.add(Arguments.of(applicationStatus1, applicationStatus2));
            }
        }
        return argumentBuilder.build();
    }

    @Test
    public void testApplicationUsesPassedStatus() {
        Application application = new Application("X", "Y", ApplicationStatus.APPLIED, "", "");
        List<StatusChange> statusChanges = application.getStatusChanges();
        assertNotNull(statusChanges);
        assertFalse(statusChanges.isEmpty());
        assertEquals(ApplicationStatus.APPLIED, statusChanges.getFirst().getStatus());
        assertEquals(1, statusChanges.size());
        assertEquals(StatusChangeCause.MANUAL, statusChanges.getFirst().getCause());
    }
    @Test
    public void testApplicationImmediateStatusChange() {
        Application application = new Application("X", "Y", ApplicationStatus.SAVED, "", "");
        application.changeStatus(ApplicationStatus.APPLIED);
        assertEquals(ApplicationStatus.APPLIED, application.getStatusChanges().getLast().getStatus());
    }
}
