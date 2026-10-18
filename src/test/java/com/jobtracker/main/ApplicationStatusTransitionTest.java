package com.jobtracker.main;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ApplicationController.class)
@Import({SecurityConfig.class, PasswordConfig.class})
public class ApplicationStatusTransitionTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    public void postFalseStatus_returns409() throws Exception {
        String json = """
                {
                    "status": "SAVED"
                }""";
        UUID id = UUID.fromString("01a1168a-fe8a-7df8-8228-0158fcc22d99");
        ChangeStatusDto changeStatusDto = new ChangeStatusDto(ApplicationStatus.SAVED);
        UUID userId = UUID.fromString("01a1168a-fe8a-7df8-8228-0158fcc22d90");
        UserDetailModel userDetailModel = new UserDetailModel(userId, "a", "b");
        when(applicationService.changeStatus(id, changeStatusDto, userId))
                .thenThrow(new IllegalStatusTransitionException("Cannot change from status SAVED to status SAVED"));
        mockMvc.perform(post("/applications/"+ id +"/status")
                        .with(csrf())
                        .with(user(userDetailModel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("detail").value("Cannot change from status SAVED to status SAVED"))
                .andExpect(jsonPath("instance").value("/applications/"+ id +"/status"))
                .andExpect(jsonPath("status").value(409))
                .andExpect(jsonPath("title").value("Conflict"));
        verify(applicationService, times(1)).changeStatus(id, changeStatusDto, userId);
    }
}
