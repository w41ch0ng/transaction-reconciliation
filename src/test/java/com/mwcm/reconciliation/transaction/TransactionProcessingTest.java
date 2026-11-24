package com.mwcm.reconciliation.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=true")
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

    @MockitoSpyBean
    private TransactionService transactions;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.transactions-dead-letter-topic}")
    private String deadLetterTopic;

    @Value("${app.kafka.processing-attempts}")
    private int processingAttempts;

    @Autowired
    TransactionProcessingTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void resetProcessing() {
        reset(transactions);
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

        assertEquals("PROCESSED", waitForStatus(transactionId, "PROCESSED"));
        mockMvc.perform(get("/api/v1/transactions").param("sourceId", sourceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void processingSucceedsAfterARetry() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
            if (attempts.getAndIncrement() < processingAttempts - 1) {
                throw new IllegalStateException("processing failed");
            }
            return invocation.callRealMethod();
        }).when(transactions).process(any(UUID.class));

        String transactionId = submit("RETRY");

        assertEquals("PROCESSED", waitForStatus(transactionId, "PROCESSED"));
    }

    @Test
    void processingFailureIsRecorded() throws Exception {
        doThrow(new IllegalStateException("processing failed"))
            .when(transactions).process(any(UUID.class));

        String transactionId = submit("FAIL");

        assertEquals("FAILED", waitForStatus(transactionId, "FAILED"));
        assertTrue(deadLetterContains(transactionId));
    }

    private String submit(String prefix) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String sourceId = createSource(prefix + "-" + suffix, "Core ledger");
        String created = mockMvc.perform(post("/api/v1/sources/" + sourceId + "/transactions")
                .header("Idempotency-Key", prefix.toLowerCase() + "-" + suffix)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRANSACTION_JSON))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        return JsonPath.read(created, "$.id");
    }

    private String waitForStatus(String transactionId, String expected) throws Exception {
        String observed = "";
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (System.nanoTime() < deadline) {
            String body = mockMvc.perform(get("/api/v1/transactions/" + transactionId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
            observed = JsonPath.read(body, "$.status");
            if (expected.equals(observed)) {
                return observed;
            }
            Thread.sleep(200);
        }
        return observed;
    }

    private boolean deadLetterContains(String transactionId) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(List.of(deadLetterTopic));
            long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
            while (System.nanoTime() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));
                for (ConsumerRecord<String, String> record : records) {
                    if (transactionId.equals(record.value())) {
                        return true;
                    }
                }
            }
        }
        return false;
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
