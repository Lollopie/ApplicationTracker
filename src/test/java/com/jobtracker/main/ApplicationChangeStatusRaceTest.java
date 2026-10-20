package com.jobtracker.main;

import com.jobtracker.main.jobapplications.ApplicationController;
import com.jobtracker.main.jobapplications.ApplicationService;
import com.jobtracker.main.jobapplications.ApplicationStatus;
import com.jobtracker.main.jobapplications.ChangeStatusDto;
import com.jobtracker.main.users.UserDetailModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ApplicationController.class)
@Import({SecurityConfig.class, PasswordConfig.class, UnauthorizedAuthenticationEntryPoint.class})
public class ApplicationChangeStatusRaceTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    public void postWithLockFailure_returns409() throws Exception {
        UUID id = UUID.fromString("01a1168a-fe8a-7df8-8228-0158fcc22d99");
        String json = """
                {
                    "status": "SAVED"
                }""";
        ChangeStatusDto changeStatusDto = new ChangeStatusDto(ApplicationStatus.SAVED);
        UUID userId = UUID.fromString("01a1168a-fe8a-7df8-8228-0158fcc22d90");
        UserDetailModel userDetailModel = new UserDetailModel(userId, "a", "b");
        when(applicationService.changeStatus(id, changeStatusDto, userId)).thenThrow(ObjectOptimisticLockingFailureException.class);
        mockMvc.perform(post("/applications/" + id + "/status").with(csrf())
                        .with(user(userDetailModel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("detail").value("Requested object has been recently modified, reload and try again"))
                .andExpect(jsonPath("instance").value("/applications/" + id + "/status"))
                .andExpect(jsonPath("status").value(HttpStatus.CONFLICT.value()))
                .andExpect(jsonPath("title").value("Conflict"));
    }
}
