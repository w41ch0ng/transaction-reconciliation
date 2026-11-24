package com.mwcm.reconciliation.transaction;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class TransactionProcessingErrorHandler {
    @Bean
    DefaultErrorHandler kafkaProcessingErrorHandler(
            TransactionProcessingRecoverer recoverer,
            @Value("${app.kafka.processing-attempts}") int attempts,
            @Value("${app.kafka.processing-retry-interval}") Duration interval) {
        if (attempts < 1) {
            throw new IllegalArgumentException("processing attempts must be at least 1");
        }
        return new DefaultErrorHandler(recoverer, new FixedBackOff(interval.toMillis(), attempts - 1L));
    }
}
