package com.jobtracker.main;

import com.jobtracker.main.users.LoginDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Behind the path-based gateway the cookies must be scoped to the app's prefix and must not use the default names. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "COOKIE_PATH=/tracker",
        "SESSION_COOKIE_NAME=JT_SESSION"
})
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@Import({TestData.class, PasswordConfig.class})
public class CookiePathIT extends AbstractIntegrationTest {

    @Autowired
    TestData testData;

    @Autowired
    RestTestClient client;

    @BeforeEach
    public void cleanUp() {
        testData.clear();
    }

    @Test
    public void cookiesAreScopedToTheConfiguredPath() {
        testData.scenario().withUser("user", "password");
        var csrf = client.get().uri("/csrf").exchange().returnResult(Void.class);
        ResponseCookie csrfCookie = Objects.requireNonNull(csrf.getResponseCookies().getFirst("XSRF-TOKEN"));
        assertEquals("/tracker", csrfCookie.getPath());

        String token = csrfCookie.getValue();
        var login = client.post().uri("/login")
                .cookie("XSRF-TOKEN", token)
                .header("X-XSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginDto("user", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectCookie().exists("JT_SESSION")
                .returnResult(Void.class);
        ResponseCookie session = Objects.requireNonNull(login.getResponseCookies().getFirst("JT_SESSION"));
        assertEquals("/tracker", session.getPath());
        // The token rotation on login clears the cookie at the same path, otherwise the browser would keep the stale one.
        ResponseCookie clearedCsrf = Objects.requireNonNull(login.getResponseCookies().getFirst("XSRF-TOKEN"));
        assertEquals("/tracker", clearedCsrf.getPath());
        assertEquals(0, clearedCsrf.getMaxAge().getSeconds());
    }
}
