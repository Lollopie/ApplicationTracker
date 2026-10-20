package com.jobtracker.main;

import com.jobtracker.main.jobapplications.*;
import com.jobtracker.main.users.UserDetailModel;
import com.jobtracker.main.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = JobApplicationController.class)
@Import({SecurityConfig.class, PasswordConfig.class, UnauthorizedAuthenticationEntryPoint.class})
public class JobApplicationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobApplicationService jobApplicationService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private UserRepository userRepository;
    UUID userId = UUID.fromString("01a1168a-fe8a-7df8-8228-0158fcc22d90");
    UserDetailModel userDetailModel = new UserDetailModel(userId, "a", "b");
    @Test
    public void postWithoutCompany_returns400() throws Exception {
        String json = """
                {
                    "company": null,
                    "position": "X",
                    "status": "SAVED"
                }""";

        mockMvc.perform(post("/applications")
                        .with(csrf())
                        .with(user(userDetailModel))
                        .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors.company[0]").value("Company is required"));
        verify(jobApplicationService, never()).saveApplication(any(), any());
    }

    @Test
    public void postWithBlankCompany_returns400() throws Exception {
        String json = """
                {
                    "company": "    ",
                    "position": "X",
                    "status": "SAVED"
                }""";
        mockMvc.perform(post("/applications")
                        .with(csrf())
                        .with(user(userDetailModel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors.company[0]").value("Company is required"));
        verify(jobApplicationService, never()).saveApplication(any(), any());
    }

    @Test
    public void postWithInvalidStatus_returns400() throws Exception {
        String json = """
                {
                    "company": "X",
                    "position": "X",
                    "status": "BANANA"
                }""";
        mockMvc.perform(post("/applications")
                        .with(csrf())
                        .with(user(userDetailModel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("error")
                        .value("\"BANANA\": not one of the values accepted for status: [SAVED, APPLIED, INTERVIEW, OFFER, REJECTED]."));
        verify(jobApplicationService, never()).saveApplication(any(), any());
    }

    @Test
    public void postWithEmptyBody_returns400() throws Exception {
        mockMvc.perform(post("/applications")
                        .with(csrf())
                        .with(user(userDetailModel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"));
        verify(jobApplicationService, never()).saveApplication(any(), any());
    }

    @Test
    public void getWithInvalidId_returns404() throws Exception {
        UUID id = UUID.fromString("01a1168a-fe8a-7df8-8228-0158fcc22d99");
        String url = "/applications/" + id;
        when(jobApplicationService.getApplicationById(id, userId)).thenReturn(Optional.empty());
        mockMvc.perform(get(url).with(user(userDetailModel)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("detail").value("Application Not Found"))
                .andExpect(jsonPath("instance").value(url))
                .andExpect(jsonPath("status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("title").value("Not Found"));
        verify(jobApplicationService, times(1)).getApplicationById(id, userId);
    }

    @Test
    public void postWithValidData_returns201() throws Exception {
        String json = """
                {
                    "company": "X",
                    "position": "Y",
                    "status": "SAVED"
                }""";
        UUID id = UUID.fromString("01a1168a-fe8a-7df8-8228-0158fcc22d99");
        JobApplicationDto jobApplicationDto = new JobApplicationDto("X", "Y", JobApplicationStatus.SAVED, null, null);
        JobApplicationResponseDto jobApplicationResponseDto = new JobApplicationResponseDto(id, "X", "Y",
                null, null, List.of(new ChangeStatusResponseDto(JobApplicationStatus.SAVED, StatusChangeCause.MANUAL, "", Instant.now())));
        when(jobApplicationService.saveApplication(jobApplicationDto, userId)).thenReturn(jobApplicationResponseDto);
        mockMvc.perform(post("/applications")
                        .with(csrf())
                        .with(user(userDetailModel))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/applications/" + id))
                .andExpect(jsonPath("id").value(id.toString()))
                .andExpect(jsonPath("company").value("X"))
                .andExpect(jsonPath("position").value("Y"))
                .andExpect(jsonPath("notes").doesNotExist())
                .andExpect(jsonPath("links").doesNotExist())
                .andExpect(jsonPath("statusHistory[0].status").value("SAVED"))
                .andExpect(jsonPath("statusHistory[0].cause").value("MANUAL"))
                .andExpect(jsonPath("statusHistory[0].detail").value(""))
                .andExpect(jsonPath("statusHistory[0].createdAt").exists());
        verify(jobApplicationService, times(1)).saveApplication(jobApplicationDto, userId);
    }

    @Test
    public void postWithMalformedJson_returns400() throws Exception {
        String json = """
                {
                    "company": "X",
                    "position": "X",
                    "status": "SAVED"
                """;
        mockMvc.perform(post("/applications")
                        .with(csrf())
                        .with(user(userDetailModel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("error").value("Malformed JSON request"));
        verify(jobApplicationService, never()).saveApplication(any(), any());
    }

    @Test
    public void postWithNoCsrf_returns403() throws Exception {
        String json = """
                {
                    "company": "X",
                    "position": "X",
                    "status": "SAVED"
                """;
        mockMvc.perform(post("/applications")
                        .with(user(userDetailModel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
        verify(jobApplicationService, never()).saveApplication(any(), any());
    }
    @Test
    public void postWithNoAuthentication_returns401() throws Exception {
        String json = """
                {
                    "company": "X",
                    "position": "X",
                    "status": "SAVED"
                """;
        mockMvc.perform(post("/applications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
        verify(jobApplicationService, never()).saveApplication(any(), any());
    }

    @Test
    public void getWithParameterBinding_returns200() throws Exception {
        Pageable pageable = PageRequest.of(1, 5, Sort.Direction.DESC, "currentStatus");
        when(jobApplicationService.getApplications(userId, pageable, JobApplicationStatus.SAVED, "APPLE", "JDEV")).thenReturn(null);
        mockMvc.perform(get("/applications?currentStatus=SAVED&company=APPLE&position=JDEV&sort=currentStatus,desc&page=1&size=5")
                        .with(user(userDetailModel)))
                .andExpect(status().isOk());
        verify(jobApplicationService, times(1)).getApplications(userId, pageable, JobApplicationStatus.SAVED, "APPLE", "JDEV");
    }
    @Test
    public void getWithBadSort_returns400() throws Exception {
        mockMvc.perform(get("/applications?sort=version")
                        .with(user(userDetailModel)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("detail").value("Illegal Sort by field"));
        verify(jobApplicationService, never()).getApplications(any(), any(), any(), any(), any());
    }
}
