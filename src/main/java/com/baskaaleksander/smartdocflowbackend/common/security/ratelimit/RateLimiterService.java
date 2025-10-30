package com.baskaaleksander.smartdocflowbackend.common.security.ratelimit;

import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiterService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key, RatePolicy policy) {
        return cache.computeIfAbsent(key + ":" + policy.name(), k -> createBucket(policy));
    }

    private Bucket createBucket(RatePolicy policy) {
        return null;
    }
}
