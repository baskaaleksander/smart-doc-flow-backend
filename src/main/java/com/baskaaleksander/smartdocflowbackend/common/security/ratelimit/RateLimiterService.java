package com.baskaaleksander.smartdocflowbackend.common.security.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiterService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key, RatePolicy policy) {
        return cache.computeIfAbsent(key + ":" + policy.name(), k -> createBucket(policy));
    }

    private Bucket createBucket(RatePolicy policy) {

        Bandwidth limit = switch (policy) {
            case READ -> Bandwidth.builder()
                    .capacity(100)
                    .refillIntervally(100, Duration.ofMinutes(1))
                    .build();
            case WRITE -> Bandwidth.builder()
                    .capacity(20)
                    .refillIntervally(20, Duration.ofMinutes(1))
                    .build();
            case STRICT -> Bandwidth.builder()
                    .capacity(5)
                    .refillIntervally(5, Duration.ofMinutes(1))
                    .build();
            default -> throw new IllegalArgumentException("Unknown policy " + policy);
        };
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
