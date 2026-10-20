package com.jobtracker.main;

import com.jobtracker.main.jobapplications.JobApplication;
import com.jobtracker.main.jobapplications.JobApplicationRepository;
import com.jobtracker.main.jobapplications.JobApplicationService;
import com.jobtracker.main.jobapplications.JobApplicationStatus;
import com.jobtracker.main.users.User;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestData.class, PasswordConfig.class})
public class JobApplicationFilterIT extends AbstractIntegrationTest {

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    TestData testData;

    @Autowired
    TestEntityManager entityManager;

    @Test
    public void filtersApplicationsByOwner() {
        TestData.Scenario scenario = testData.scenario()
                .withUser("user1")
                .withUser("user2")
                .withApplication("1", "1", JobApplicationStatus.SAVED, null, null, "user1")
                .withApplication("2", "2", JobApplicationStatus.SAVED, null, null, "user2");
        entityManager.clear();
        User user1 = scenario.getUser("user1");
        User user2 = scenario.getUser("user2");
        List<JobApplication> jobApplications = scenario.applications();
        JobApplication jobApplication1 = jobApplications.get(0);
        JobApplication jobApplication2 = jobApplications.get(1);
        Pageable pageable = Pageable.ofSize(10);
        Page<JobApplication> user1Applications = jobApplicationRepository.findAll((root, query, builder) ->
                JobApplicationService.getFilter(root, query, builder, user1.getId(), null, null, null), pageable);
        assertEquals(jobApplication1.getCompany(), user1Applications.get().toList().getFirst().getCompany());
        assertEquals(1, user1Applications.getTotalElements());
        Page<JobApplication> user2Applications = jobApplicationRepository.findAll((root, query, builder) ->
                JobApplicationService.getFilter(root, query, builder, user2.getId(), null, null, null), pageable);
        assertEquals(jobApplication2.getCompany(), user2Applications.get().toList().getFirst().getCompany());
        assertEquals(1, user2Applications.getTotalElements());
    }

    @Test
    public void sortsApplicationsByCompany() {
        TestData.Scenario scenario = testData.scenario()
                .withUser("user1")
                .withApplication("b", "1", JobApplicationStatus.SAVED, null, null, "user1")
                .withApplication("a", "1", JobApplicationStatus.SAVED, null, null, "user1")
                .withApplication("d", "1", JobApplicationStatus.SAVED, null, null, "user1")
                .withApplication("c", "1", JobApplicationStatus.SAVED, null, null, "user1")
                .withApplication("e", "1", JobApplicationStatus.SAVED, null, null, "user1");
        entityManager.clear();
        User user1 = scenario.getUser("user1");
        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.ASC, "company");
        Page<JobApplication> user1Applications = jobApplicationRepository.findAll((root, query, builder) ->
                JobApplicationService.getFilter(root, query, builder, user1.getId(), null, null, null), pageable);
        List<JobApplication> jobApplications = user1Applications.get().toList();
        for(int i = 0; i < 5; i++) {
            assertEquals(String.valueOf(Character.toChars('a' + i)), jobApplications.get(i).getCompany());
        }
    }

    @Test
    public void pagingWithMultipleStatuses() {
        List<JobApplicationStatus> statusList = List.of(JobApplicationStatus.SAVED,
                JobApplicationStatus.APPLIED, JobApplicationStatus.INTERVIEW, JobApplicationStatus.INTERVIEW,
                JobApplicationStatus.OFFER, JobApplicationStatus.REJECTED);
        TestData.Scenario scenario = testData.scenario()
                .withUser("user1")
                .withApplication("1", "1", statusList, null, null, "user1")
                .withApplication("2", "1", statusList, null, null, "user1")
                .withApplication("3", "1", statusList, null, null, "user1");
        entityManager.clear();
        User user1 = scenario.getUser("user1");
        Pageable pageable = PageRequest.of(0, 2, Sort.Direction.ASC, "company");
        Page<JobApplication> user1Applications = jobApplicationRepository.findAll((root, query, builder) ->
                JobApplicationService.getFilter(root, query, builder, user1.getId(), null, null, null), pageable);
        assertEquals(2, user1Applications.getNumberOfElements());
        assertEquals(3, user1Applications.getTotalElements());
        List<JobApplication> jobApplications = user1Applications.getContent();
        assertEquals(6, jobApplications.get(0).getStatusChanges().size());
        assertEquals(6, jobApplications.get(1).getStatusChanges().size());
    }

    @Test
    public void duplicateUserName() {
        TestData.Scenario scenario = testData.scenario()
                .withUser("user1");
        entityManager.clear();
        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class, () -> {
            scenario.withUser("user1");
        });
        Throwable root = exception.getMostSpecificCause();
        assertInstanceOf(PSQLException.class, root);
        assertEquals("23505", ((PSQLException) root).getSQLState());
        assertTrue(root.getMessage().contains("uq_user_username"));
    }
}
