package com.baskaaleksander.smartdocflowbackend.common.security.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiterService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.read}")
    private long READ_LIMIT;

    @Value("${app.rate-limit.write}")
    private long WRITE_LIMIT;

    @Value("${app.rate-limit.strict}")
    private long STRICT_LIMIT;

    public Bucket resolveBucket(String key, RatePolicy policy) {
        return cache.computeIfAbsent(key + ":" + policy.name(), k -> createBucket(policy));
    }

    private Bucket createBucket(RatePolicy policy) {

        Bandwidth limit = switch (policy) {
            case READ -> Bandwidth.builder()
                    .capacity(READ_LIMIT)
                    .refillIntervally(READ_LIMIT, Duration.ofMinutes(1))
                    .build();
            case WRITE -> Bandwidth.builder()
                    .capacity(WRITE_LIMIT)
                    .refillIntervally(WRITE_LIMIT, Duration.ofMinutes(1))
                    .build();
            case STRICT -> Bandwidth.builder()
                    .capacity(STRICT_LIMIT)
                    .refillIntervally(STRICT_LIMIT, Duration.ofMinutes(1))
                    .build();
            default -> throw new IllegalArgumentException("Unknown policy " + policy);
        };
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
