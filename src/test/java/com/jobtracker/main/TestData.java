package com.jobtracker.main;

import com.jobtracker.main.jobapplications.JobApplication;
import com.jobtracker.main.jobapplications.JobApplicationRepository;
import com.jobtracker.main.jobapplications.JobApplicationStatus;
import com.jobtracker.main.users.User;
import com.jobtracker.main.users.UserRepository;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@TestComponent
public class TestData {

    private final UserRepository userRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final PasswordEncoder passwordEncoder;

    public TestData(UserRepository userRepository,
                    JobApplicationRepository jobApplicationRepository,
                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Scenario scenario() {
        return new Scenario();
    }

    public void clear() {
        jobApplicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    public class Scenario {
        private final HashMap<String, User> users = new HashMap<>();
        private final List<JobApplication> jobApplications = new ArrayList<>();

        public Scenario withUser(String username) {
            users.put(username, userRepository.saveAndFlush(new User(username, passwordEncoder.encode("password"))));
            return this;
        }

        public Scenario withUser(String username, String password) {
            users.put(username, userRepository.saveAndFlush(new User(username, passwordEncoder.encode(password))));
            return this;
        }

        public Scenario withApplication(String company, String position, JobApplicationStatus status, String notes, String links, String username) {
            User user = users.get(username);
            jobApplications.add(jobApplicationRepository.saveAndFlush(new JobApplication(company, position, status, notes, links, user)));
            return this;
        }

        public Scenario withApplication(String company, String position, List<JobApplicationStatus> statusList, String notes, String links, String username) {
            User user = users.get(username);
            JobApplication jobApplication = new JobApplication(company, position, statusList.getFirst(), notes, links, user);
            for (int i = 1; i < statusList.size(); i++) {
                jobApplication.changeStatus(statusList.get(i));
            }
            jobApplications.add(jobApplicationRepository.saveAndFlush(jobApplication));
            return this;
        }

        public User user() {
            Iterator<User> userIterator = users.values().iterator();
            return userIterator.hasNext() ? userIterator.next() : null;
        }
        public List<User> users() { return users.values().stream().toList(); }
        public User getUser(String username) { return users.get(username); }
        public JobApplication application() { return jobApplications.getFirst(); }
        public List<JobApplication> applications() { return jobApplications; }
    }
}