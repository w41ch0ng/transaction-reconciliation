package com.mwcm.reconciliation.transaction;

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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransactionControllerTest {
    private static final String TRANSACTION_JSON = """
            {
              "externalReference": "TXN-1042",
              "transactionType": "PAYMENT",
              "amountMinorUnits": 25000,
              "currency": "GBP",
              "transactionTimestamp": "2026-11-13T10:30:00Z",
              "metadata": { "channel": "card" }
            }
            """;

    private final MockMvc mockMvc;

    @Autowired
    TransactionControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void createReturnsAcceptedTransaction() throws Exception {
        String sourceId = createSource("CTRL-TX-CREATE", "Core ledger");

        mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.externalReference").value("TXN-1042"))
            .andExpect(jsonPath("$.transactionType").value("PAYMENT"))
            .andExpect(jsonPath("$.amountMinorUnits").value(25000))
            .andExpect(jsonPath("$.currency").value("GBP"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.metadata.channel").value("card"))
            .andExpect(jsonPath("$.sourceId").value(sourceId))
            .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void missingSourceReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/sources/00000000-0000-0000-0000-000000000000/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void blankReferenceReturnsBadRequest() throws Exception {
        String sourceId = createSource("CTRL-TX-BLANK", "Core ledger");
        String body = TRANSACTION_JSON.replace("TXN-1042", "");

        mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void nonPositiveAmountReturnsBadRequest() throws Exception {
        String sourceId = createSource("CTRL-TX-AMOUNT", "Core ledger");
        String body = TRANSACTION_JSON.replace("25000", "0");

        mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getByIdReturnsCreatedTransaction() throws Exception {
        String sourceId = createSource("CTRL-TX-GET", "Core ledger");
        String response = mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String transactionId = JsonPath.read(response, "$.id");

        mockMvc.perform(get("/api/v1/transactions/" + transactionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(transactionId))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void missingTransactionReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isNotFound());
    }

    @Test
    void listFiltersByExternalReference() throws Exception {
        String sourceId = createSource("CTRL-TX-LIST", "Core ledger");
        submit(sourceId, TRANSACTION_JSON);
        submit(sourceId, TRANSACTION_JSON.replace("TXN-1042", "TXN-OTHER"));

        mockMvc.perform(get("/api/v1/transactions")
                .param("externalReference", "TXN-1042")
                .param("sourceId", sourceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].externalReference").value("TXN-1042"));
    }

    @Test
    void listHonoursPageSize() throws Exception {
        String sourceId = createSource("CTRL-TX-PAGE", "Core ledger");
        submit(sourceId, TRANSACTION_JSON);
        submit(sourceId, TRANSACTION_JSON.replace("TXN-1042", "TXN-1043"));

        mockMvc.perform(get("/api/v1/transactions")
                .param("sourceId", sourceId)
                .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content.length()").value(1));
    }

    private void submit(String sourceId, String body) throws Exception {
        mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isAccepted());
    }

    private String createSource(String institutionCode, String sourceName) throws Exception {
        String institutionResponse = mockMvc.perform(post("/api/v1/institutions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Txn Bank\",\"code\":\"" + institutionCode + "\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String institutionId = JsonPath.read(institutionResponse, "$.id");

        String sourceResponse = mockMvc.perform(post("/api/v1/institutions/" + institutionId + "/sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + sourceName + "\",\"sourceType\":\"INTERNAL_LEDGER\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return JsonPath.read(sourceResponse, "$.id");
    }
}
