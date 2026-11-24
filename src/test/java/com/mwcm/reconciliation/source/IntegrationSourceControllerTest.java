package com.mwcm.reconciliation.source;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@AutoConfigureMockMvc
@Transactional
class IntegrationSourceControllerTest {
    private final MockMvc mockMvc;

    @Autowired
    IntegrationSourceControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void createReturnsCreatedSource() throws Exception {
        String institutionId = createInstitution("Controller Bank", "CTRL-CREATE");

        mockMvc.perform(post("/api/v1/institutions/" + institutionId + "/sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Core ledger\",\"sourceType\":\"INTERNAL_LEDGER\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Core ledger"))
            .andExpect(jsonPath("$.sourceType").value("INTERNAL_LEDGER"))
            .andExpect(jsonPath("$.institutionId").value(institutionId))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void duplicateNameReturnsConflict() throws Exception {
        String institutionId = createInstitution("Duplicate Bank", "CTRL-DUP");
        String body = "{\"name\":\"Core ledger\",\"sourceType\":\"INTERNAL_LEDGER\"}";

        mockMvc.perform(post("/api/v1/institutions/" + institutionId + "/sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/institutions/" + institutionId + "/sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("name already in use"));
    }

    @Test
    void listByInstitutionIncludesCreatedSource() throws Exception {
        String institutionId = createInstitution("List Bank", "CTRL-LIST");

        mockMvc.perform(post("/api/v1/institutions/" + institutionId + "/sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Card processor\",\"sourceType\":\"PAYMENT_PROCESSOR\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/institutions/" + institutionId + "/sources"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name").value(hasItem("Card processor")));
    }

    @Test
    void getByIdReturnsCreatedSource() throws Exception {
        String institutionId = createInstitution("Lookup Bank", "CTRL-GET");
        String response = mockMvc.perform(post("/api/v1/institutions/" + institutionId + "/sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Core ledger\",\"sourceType\":\"INTERNAL_LEDGER\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String sourceId = JsonPath.read(response, "$.id");

        mockMvc.perform(get("/api/v1/sources/" + sourceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(sourceId))
            .andExpect(jsonPath("$.name").value("Core ledger"));
    }

    @Test
    void missingSourceReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/sources/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isNotFound());
    }

    @Test
    void missingInstitutionReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/institutions/00000000-0000-0000-0000-000000000000/sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Core ledger\",\"sourceType\":\"INTERNAL_LEDGER\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void blankNameReturnsBadRequest() throws Exception {
        String institutionId = createInstitution("Blank Bank", "CTRL-BLANK");

        mockMvc.perform(post("/api/v1/institutions/" + institutionId + "/sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"sourceType\":\"INTERNAL_LEDGER\"}"))
            .andExpect(status().isBadRequest());
    }

    private String createInstitution(String name, String code) throws Exception {
        String response = mockMvc.perform(post("/api/v1/institutions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"code\":\"" + code + "\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return JsonPath.read(response, "$.id");
    }
}
