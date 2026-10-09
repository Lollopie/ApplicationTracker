package com.jobtracker.main;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ApplicationController.class)
public class ApplicationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicationService applicationService;

    @Test
    public void postWithoutCompany_returns400() throws Exception {
        String json = """
                {
                    "company": null,
                    "position": "X",
                    "status": "SAVED"
                }""";
        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors.company[0]").value("Company is required"));
        verify(applicationService, never()).saveApplication(any());
    }

    @Test
    public void postWithBlankCompany_returns400() throws Exception {
        String json = """
                {
                    "company": "    ",
                    "position": "X",
                    "status": "SAVED"
                }""";
        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors.company[0]").value("Company is required"));
        verify(applicationService, never()).saveApplication(any());
    }

    @Test
    public void postWithInvalidStatus_returns400() throws Exception {
        String json = """
                {
                    "company": "X",
                    "position": "X",
                    "status": "BANANA"
                }""";
        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("error")
                        .value("\"BANANA\": not one of the values accepted for status: [SAVED, APPLIED, INTERVIEW, OFFER, REJECTED]."));
        verify(applicationService, never()).saveApplication(any());
    }

    @Test
    public void postWithEmptyBody_returns400() throws Exception {
        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"));
        verify(applicationService, never()).saveApplication(any());
    }

    @Test
    public void getWithInvalidId_returns404() throws Exception {
        UUID id = UUID.fromString("01a1168a-fe8a-7df8-8228-0158fcc22d99");
        String url = "/applications/" + id;
        when(applicationService.getApplicationById(id)).thenReturn(Optional.empty());
        mockMvc.perform(get(url))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("detail").value("Application Not Found"))
                .andExpect(jsonPath("instance").value(url))
                .andExpect(jsonPath("status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("title").value("Not Found"));
        verify(applicationService, times(1)).getApplicationById(id);
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
        ApplicationDto applicationDto = new ApplicationDto("X", "Y", ApplicationStatus.SAVED, null, null);
        ApplicationResponseDto applicationResponseDto = new ApplicationResponseDto(id, "X", "Y",
                null, null, List.of(new ChangeStatusResponseDto(ApplicationStatus.SAVED, StatusChangeCause.MANUAL, "", Instant.now())));
        when(applicationService.saveApplication(applicationDto)).thenReturn(applicationResponseDto);
        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON).content(json))
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
        verify(applicationService, times(1)).saveApplication(applicationDto);
    }

    @Test
    public void postWithMalformedJson_returns400() throws Exception {
        String json = """
                {
                    "company": "X",
                    "position": "X",
                    "status": "SAVED"
                """;
        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("error").value("Malformed JSON request"));
        verify(applicationService, never()).saveApplication(any());
    }
}
