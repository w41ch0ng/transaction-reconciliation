package com.mwcm.reconciliation.institution;

import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;

import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@AutoConfigureMockMvc
@Transactional
class InstitutionControllerTest {
    private final MockMvc mockMvc;

    @Autowired
    InstitutionControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void createReturnsCreatedInstitution() throws Exception {
        mockMvc.perform(post("/api/v1/institutions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Api Bank\",\"code\":\"API-BANK\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("API-BANK"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void createConflictingInstitutions() throws Exception {
        mockMvc.perform(post("/api/v1/institutions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Api Dup\",\"code\":\"API-DUP\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("API-DUP"))
                .andExpect(jsonPath("$.id").isNotEmpty());
        mockMvc.perform(post("/api/v1/institutions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Api Dup\",\"code\":\"API-DUP\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("code already in use"));
    }

    @Test
    void getAllInstitutionsReturnsAll() throws Exception {
        mockMvc.perform(post("/api/v1/institutions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"List Bank\",\"code\":\"API-LIST\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/institutions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].code").value(hasItem("API-LIST")));
    }

    @Test
    void getMissingInstitutionReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/institutions/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isNotFound());
    }

    @Test
    void blankNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/institutions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"\",\"code\":\"API-BANK\"}"))
                .andExpect(status().isBadRequest());
    }
}
