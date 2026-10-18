package com.jobtracker.main;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@TestComponent
public class TestData {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;

    public TestData(UserRepository userRepository,
                    ApplicationRepository applicationRepository,
                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Scenario scenario() {
        return new Scenario();
    }

    public void clear() {
        applicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    public class Scenario {
        private final HashMap<String, User> users = new HashMap<>();
        private final List<Application> applications = new ArrayList<>();

        public Scenario withUser(String username) {
            users.put(username, userRepository.saveAndFlush(new User(username, passwordEncoder.encode("password"))));
            return this;
        }

        public Scenario withUser(String username, String password) {
            users.put(username, userRepository.saveAndFlush(new User(username, passwordEncoder.encode(password))));
            return this;
        }

        public Scenario withApplication(String company, String position, ApplicationStatus status, String notes, String links, String username) {
            User user = users.get(username);
            applications.add(applicationRepository.saveAndFlush(new Application(company, position, status, notes, links, user)));
            return this;
        }

        public Scenario withApplication(String company, String position, List<ApplicationStatus> statusList, String notes, String links, String username) {
            User user = users.get(username);
            Application application = new Application(company, position, statusList.getFirst(), notes, links, user);
            for (int i = 1; i < statusList.size(); i++) {
                application.changeStatus(statusList.get(i));
            }
            applications.add(applicationRepository.saveAndFlush(application));
            return this;
        }

        public User user() {
            Iterator<User> userIterator = users.values().iterator();
            return userIterator.hasNext() ? userIterator.next() : null;
        }
        public List<User> users() { return users.values().stream().toList(); }
        public User getUser(String username) { return users.get(username); }
        public Application application() { return applications.getFirst(); }
        public List<Application> applications() { return applications; }
    }
}