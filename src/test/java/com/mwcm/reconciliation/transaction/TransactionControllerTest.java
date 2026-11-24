package com.mwcm.reconciliation.transaction;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
class TransactionControllerTest {
    private static final String TRANSACTION_JSON = """
            {
              "externalReference": "TXN-1042",
              "transactionType": "PAYMENT",
              "amountMinorUnits": 25000,
              "currency": "GBP",
              "transactionTimestamp": "2026-11-13T10:30:00Z",
              "status": "COMPLETED",
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
                .header("Idempotency-Key", "create-1042")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.idempotencyKey").value("create-1042"))
            .andExpect(jsonPath("$.externalReference").value("TXN-1042"))
            .andExpect(jsonPath("$.transactionType").value("PAYMENT"))
            .andExpect(jsonPath("$.amountMinorUnits").value(25000))
            .andExpect(jsonPath("$.currency").value("GBP"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.providerStatus").value("COMPLETED"))
            .andExpect(jsonPath("$.normalisedStatus").value("COMPLETED"))
            .andExpect(jsonPath("$.metadata.channel").value("card"))
            .andExpect(jsonPath("$.sourceId").value(sourceId))
            .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void normalisesProviderVocabulary() throws Exception {
        String sourceId = createSource("CTRL-TX-NORM", "Core ledger");
        String body = """
                {
                  "externalReference": "TXN-SETTLED",
                  "transactionType": "SALE",
                  "amountMinorUnits": 1250,
                  "currency": "gbp",
                  "transactionTimestamp": "2026-11-13T10:30:00Z",
                  "status": "settled"
                }
                """;

        mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .header("Idempotency-Key", "norm-settled")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.transactionType").value("PAYMENT"))
            .andExpect(jsonPath("$.currency").value("GBP"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.providerStatus").value("settled"))
            .andExpect(jsonPath("$.normalisedStatus").value("COMPLETED"));
    }

    @Test
    void unknownProviderValueReturnsBadRequest() throws Exception {
        String sourceId = createSource("CTRL-TX-BAD", "Core ledger");
        String body = TRANSACTION_JSON.replace("PAYMENT", "WIRE");

        mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .header("Idempotency-Key", "bad-wire")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("unrecognised transaction type: WIRE"));
    }

    @Test
    void missingSourceReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/sources/00000000-0000-0000-0000-000000000000/transactions")
                .header("Idempotency-Key", "missing-source")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void blankReferenceReturnsBadRequest() throws Exception {
        String sourceId = createSource("CTRL-TX-BLANK", "Core ledger");
        String body = TRANSACTION_JSON.replace("TXN-1042", "");

        mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .header("Idempotency-Key", "blank-ref")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void nonPositiveAmountReturnsBadRequest() throws Exception {
        String sourceId = createSource("CTRL-TX-AMOUNT", "Core ledger");
        String body = TRANSACTION_JSON.replace("25000", "0");

        mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .header("Idempotency-Key", "bad-amount")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getByIdReturnsCreatedTransaction() throws Exception {
        String sourceId = createSource("CTRL-TX-GET", "Core ledger");
        String response = mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .header("Idempotency-Key", "get-1042")
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
        mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .header("Idempotency-Key", "list-1042")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON))
            .andExpect(status().isAccepted());
        mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .header("Idempotency-Key", "list-other")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON.replace("TXN-1042", "TXN-OTHER")))
            .andExpect(status().isAccepted());

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
        mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .header("Idempotency-Key", "page-1042")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON))
            .andExpect(status().isAccepted());
        mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .header("Idempotency-Key", "page-1043")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON.replace("TXN-1042", "TXN-1043")))
            .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/transactions")
                .param("sourceId", sourceId)
                .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void repeatedSubmissionReturnsTheOriginalTransaction() throws Exception {
        String sourceId = createSource("CTRL-TX-REPLAY", "Core ledger");

        String first = mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .header("Idempotency-Key", "replay-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String transactionId = JsonPath.read(first, "$.id");

        mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .header("Idempotency-Key", "replay-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.id").value(transactionId));

        mockMvc.perform(get("/api/v1/transactions").param("sourceId", sourceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void sameKeyWithDifferentAmountReturnsConflict() throws Exception {
        String sourceId = createSource("CTRL-TX-CONFLICT", "Core ledger");
        mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .header("Idempotency-Key", "conflict-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON))
            .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .header("Idempotency-Key", "conflict-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON.replace("25000", "26000")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("idempotency key already used with a different request"));
    }

    @Test
    void sameKeyOnAnotherSourceCreatesASecondTransaction() throws Exception {
        String firstSource = createSource("CTRL-TX-KEY-A", "Core ledger");
        String secondSource = createSource("CTRL-TX-KEY-B", "Card processor");

        String first = mockMvc.perform(post("/api/v1/sources/" + firstSource + "/transactions")
                .header("Idempotency-Key", "shared-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String second = mockMvc.perform(post("/api/v1/sources/" + secondSource + "/transactions")
                .header("Idempotency-Key", "shared-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String firstId = JsonPath.read(first, "$.id");
        String secondId = JsonPath.read(second, "$.id");
        assertNotEquals(firstId, secondId);
    }

    @Test
    void missingIdempotencyKeyReturnsBadRequest() throws Exception {
        String sourceId = createSource("CTRL-TX-NO-KEY", "Core ledger");

        mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON))
            .andExpect(status().isBadRequest());
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
