package com.mwcm.reconciliation.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionProcessingTest {
    private static final String TRANSACTION_JSON = """
            {
              "externalReference": "TXN-1042",
              "transactionType": "PAYMENT",
              "amountMinorUnits": 25000,
              "currency": "GBP",
              "transactionTimestamp": "2026-11-13T10:30:00Z",
              "status": "COMPLETED"
            }
            """;

    private final MockMvc mockMvc;

    @Autowired
    TransactionProcessingTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void acceptedTransactionIsProcessed() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String sourceId = createSource("PROC-" + suffix, "Core ledger");

        String created = mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .header("Idempotency-Key", "process-" + suffix)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        String transactionId = JsonPath.read(created, "$.id");

        mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .header("Idempotency-Key", "process-" + suffix)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.id").value(transactionId));

        String observed = "PENDING";
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (System.nanoTime() < deadline) {
            String body = mockMvc.perform(get("/api/v1/transactions/" + transactionId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
            observed = JsonPath.read(body, "$.status");
            if ("PROCESSED".equals(observed)) {
                break;
            }
            Thread.sleep(200);
        }

        assertEquals("PROCESSED", observed);
        mockMvc.perform(get("/api/v1/transactions").param("sourceId", sourceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
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
