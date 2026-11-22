package com.mwcm.reconciliation.transaction;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyStore {
    private final StringRedisTemplate redis;
    private final Duration timeToLive;

    public IdempotencyStore(
            StringRedisTemplate redis,
            @Value("${app.idempotency.time-to-live}") Duration timeToLive) {
        this.redis = redis;
        this.timeToLive = timeToLive;
    }

    public Optional<UUID> find(UUID sourceId, String idempotencyKey) {
        String transactionId = redis.opsForValue().get(redisKey(sourceId, idempotencyKey));
        if (transactionId == null) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(transactionId));
    }

    public void remember(UUID sourceId, String idempotencyKey, UUID transactionId) {
        redis.opsForValue().set(redisKey(sourceId, idempotencyKey), transactionId.toString(), timeToLive);
    }

    private String redisKey(UUID sourceId, String idempotencyKey) {
        return "idempotency:" + sourceId + ":" + idempotencyKey;
    }
}
