package com.jobtracker.main;

import com.jobtracker.main.jobapplications.*;
import com.jobtracker.main.users.LoginDto;
import com.jobtracker.main.users.RegisterDto;
import com.jobtracker.main.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Base64;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@Import({TestData.class, PasswordConfig.class})
public class JobApplicationIT extends AbstractIntegrationTest {

    @Autowired
    TestData testData;

    @Autowired
    RestTestClient client;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JobApplicationRepository jobApplicationRepository;

    @Autowired
    JdbcClient jdbcClient;

    @BeforeEach
    public void cleanUp() {
        testData.clear();
    }

    public String getCsrfToken() {
        var result = client.get().uri("/csrf").exchange().returnResult(Void.class);
        return Objects.requireNonNull(result.getResponseCookies().getFirst("XSRF-TOKEN")).getValue();
    }

    public ResponseCookie loginAndGetSessionCookie(String token) {
        var login = client.post().uri("/login")
                .cookie("XSRF-TOKEN", token)
                .header("X-XSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginDto("user", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectCookie().exists("JSESSIONID")
                .returnResult(Void.class);
        ResponseCookie session = login.getResponseCookies().getFirst("JSESSIONID");
        assertNotNull(session);
        return session;
    }

    public ResponseCookie loginAndGetSessionCookie(String token, String username, String password) {
        testData.scenario().withUser("user", "password")
                .withApplication("X", "Y", JobApplicationStatus.SAVED, null, null, "user");

        var login = client.post().uri("/login")
                .cookie("XSRF-TOKEN", token)
                .header("X-XSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginDto(username, password))
                .exchange()
                .expectStatus().isOk()
                .expectCookie().exists("JSESSIONID")
                .returnResult(Void.class);
        ResponseCookie session = login.getResponseCookies().getFirst("JSESSIONID");
        assertNotNull(session);
        return session;
    }

    @Test
    public void register() {
        var result = client.get().uri("/csrf").exchange().returnResult(Void.class);
        String token = Objects.requireNonNull(result.getResponseCookies().getFirst("XSRF-TOKEN")).getValue();
        client.post().uri("/register")
                .cookie("XSRF-TOKEN", token)
                .header("X-XSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterDto("user1", "password"))
                .exchange()
                .expectStatus().isCreated();
        assertEquals(1, userRepository.findAll().size());
    }

    @Test
    public void login() {
        testData.scenario().withUser("user", "password");
        var result = client.get().uri("/csrf").exchange().returnResult(Void.class);
        String token = Objects.requireNonNull(result.getResponseCookies().getFirst("XSRF-TOKEN")).getValue();
        client.post().uri("/login")
                .cookie("XSRF-TOKEN", token)
                .header("X-XSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginDto("user", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectCookie().exists("JSESSIONID");
    }

    @Test
    public void me() {
        testData.scenario().withUser("user", "password");
        String token = getCsrfToken();
        ResponseCookie session = loginAndGetSessionCookie(token);
        client.get().uri("/me")
                .cookie(session.getName(), session.getValue())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("username").isEqualTo("user");
    }

    @Test
    public void sessionIsStoredInDatabase() {
        testData.scenario().withUser("user", "password");
        String token = getCsrfToken();
        ResponseCookie session = loginAndGetSessionCookie(token);
        // Spring Session's cookie carries the Base64-encoded session id.
        String sessionId = new String(Base64.getDecoder().decode(session.getValue()));
        assertEquals("user", jdbcClient.sql("SELECT principal_name FROM spring_session WHERE session_id = ?")
                .param(sessionId).query(String.class).single());
        client.get().uri("/me")
                .cookie(session.getName(), session.getValue())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void logoutDeletesStoredSession() {
        testData.scenario().withUser("user", "password");
        String token = getCsrfToken();
        ResponseCookie session = loginAndGetSessionCookie(token);
        String sessionId = new String(Base64.getDecoder().decode(session.getValue()));
        token = getCsrfToken();
        client.post().uri("/logout")
                .cookie("XSRF-TOKEN", token)
                .cookie(session.getName(), session.getValue())
                .header("X-XSRF-TOKEN", token)
                .exchange()
                .expectStatus().isOk();
        assertEquals(0, jdbcClient.sql("SELECT count(*) FROM spring_session WHERE session_id = ?")
                .param(sessionId).query(Long.class).single());
        client.get().uri("/me")
                .cookie(session.getName(), session.getValue())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void meUnauthenticated() {
        client.get().uri("/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void createApplication() {
        testData.scenario().withUser("user", "password");
        String token = getCsrfToken();
        ResponseCookie session = loginAndGetSessionCookie(token);
        client.post().uri("/applications")
                .cookie("XSRF-TOKEN", token)
                .cookie(session.getName(), session.getValue())
                .header("X-XSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new JobApplicationDto("X", "Y", JobApplicationStatus.SAVED, null, null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JobApplicationResponseDto.class);
        assertEquals(1, jobApplicationRepository.findAll().size());
    }

    @Test
    public void listApplications() {
        testData.scenario().withUser("user", "password")
                .withApplication("X", "Y", JobApplicationStatus.SAVED, null, null, "user");
        String token = getCsrfToken();
        ResponseCookie session = loginAndGetSessionCookie(token);
        client.get().uri("/applications")
                .cookie(session.getName(), session.getValue())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("content[0].id").exists()
                .jsonPath("content[0].company").isEqualTo("X")
                .jsonPath("content[0].position").isEqualTo("Y")
                .jsonPath("content[0].notes").doesNotExist()
                .jsonPath("content[0].links").doesNotExist()
                .jsonPath("content[0].statusHistory[0].status").isEqualTo("SAVED")
                .jsonPath("content[0].statusHistory[0].cause").isEqualTo("MANUAL")
                .jsonPath("content[0].statusHistory[0].detail").doesNotExist()
                .jsonPath("content[0].statusHistory[0].createdAt").exists();
    }

    @Test
    public void twoUserListApplications() {
        testData.scenario().withUser("user1")
                .withUser("user2", "password")
                .withApplication("X", "Y", JobApplicationStatus.SAVED, null, null, "user1");
        String token = getCsrfToken();
        ResponseCookie session = loginAndGetSessionCookie(token, "user2", "password");
        client.get().uri("/applications")
                .cookie(session.getName(), session.getValue())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("content[0]").doesNotExist();
    }

    @Test
    public void postWithNoSessionReturns_401() {
        testData.scenario().withUser("user", "password");
        String token = getCsrfToken();
        loginAndGetSessionCookie(token);
        client.get().uri("/applications")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void postWithNoCsrfReturns_403() {
        testData.scenario().withUser("user", "password");
        String token = getCsrfToken();
        ResponseCookie session = loginAndGetSessionCookie(token);
        client.post().uri("/applications")
                .cookie(session.getName(), session.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new JobApplicationDto("X", "Y", JobApplicationStatus.SAVED, null, null))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    public void statusChangeIsSaved() {
        TestData.Scenario scenario = testData.scenario().withUser("user", "password")
                .withApplication("X", "Y", JobApplicationStatus.SAVED, null, null, "user");
        JobApplication jobApplication = scenario.application();
        String token = getCsrfToken();
        ResponseCookie session = loginAndGetSessionCookie(token);
        token = getCsrfToken();
        client.post().uri("/applications/" + jobApplication.getApplicationId() + "/status")
                .cookie("XSRF-TOKEN", token)
                .cookie(session.getName(), session.getValue())
                .header("X-XSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChangeStatusDto(JobApplicationStatus.APPLIED))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ChangeStatusResponseDto.class);
        client.get().uri("/applications/" + jobApplication.getApplicationId())
                .cookie(session.getName(), session.getValue())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("statusHistory[1]").exists();
    }

    @Test
    public void twoUserListApplicationsById_returns404() {
        TestData.Scenario scenario = testData.scenario().withUser("user1")
                .withUser("user2", "password")
                .withApplication("X", "Y", JobApplicationStatus.SAVED, null, null, "user1");
        JobApplication jobApplication = scenario.application();
        String token = getCsrfToken();
        ResponseCookie session = loginAndGetSessionCookie(token, "user2", "password");
        client.get().uri("/applications/" + jobApplication.getApplicationId())
                .cookie(session.getName(), session.getValue())
                .exchange()
                .expectStatus().isNotFound()    ;
    }

    @Test
    public void deleteApplication() {
        TestData.Scenario scenario = testData.scenario().withUser("user", "password")
                .withApplication("X", "Y", List.of(JobApplicationStatus.SAVED, JobApplicationStatus.APPLIED, JobApplicationStatus.INTERVIEW),
                        null, null, "user");
        JobApplication jobApplication = scenario.application();
        String token = getCsrfToken();
        ResponseCookie session = loginAndGetSessionCookie(token);
        token = getCsrfToken();
        client.delete().uri("/applications/" + jobApplication.getApplicationId())
                .cookie("XSRF-TOKEN", token)
                .cookie(session.getName(), session.getValue())
                .header("X-XSRF-TOKEN", token)
                .exchange()
                .expectStatus().isNoContent();
        client.get().uri("/applications/" + jobApplication.getApplicationId())
                .cookie(session.getName(), session.getValue())
                .exchange()
                .expectStatus().isNotFound();
        assertEquals(0, jobApplicationRepository.findAll().size());
        assertEquals(0, jdbcClient.sql("SELECT count(*) FROM status_change").query(Long.class).single());
    }

    @Test
    public void twoUserDeleteApplication_returns404() {
        TestData.Scenario scenario = testData.scenario().withUser("user1")
                .withUser("user2", "password")
                .withApplication("X", "Y", JobApplicationStatus.SAVED, null, null, "user1");
        JobApplication jobApplication = scenario.application();
        String token = getCsrfToken();
        ResponseCookie session = loginAndGetSessionCookie(token, "user2", "password");
        token = getCsrfToken();
        client.delete().uri("/applications/" + jobApplication.getApplicationId())
                .cookie("XSRF-TOKEN", token)
                .cookie(session.getName(), session.getValue())
                .header("X-XSRF-TOKEN", token)
                .exchange()
                .expectStatus().isNotFound();
        assertTrue(jobApplicationRepository.findById(jobApplication.getApplicationId()).isPresent());
    }
}
