package com.jobtracker.main;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
@SpringBootTest
@ActiveProfiles("test")
class MainApplicationIT extends AbstractIntegrationTest {
	@Test
	void contextLoads() {
	}

}
