package com.baskaaleksander.smartdocflowbackend.common.security.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private final RateLimiterService rateLimiterService;

    public RateLimitingFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        RatePolicy policy = resolvePolicy(request);

        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = getClientKey(request);

        Bucket bucket = rateLimiterService.resolveBucket(clientId, policy);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long nanosToWait = probe.getNanosToWaitForRefill();
            long secondsToWait = Duration.ofNanos(nanosToWait).toSeconds();
            String path = request.getRequestURI();
            LocalDateTime now = LocalDateTime.now();

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(secondsToWait));
            response.setContentType("application/json");
            response.getWriter().write(String.format("""
                    {
                        "status": 429,
                        "message": "Too Many Requests",
                        "details": "Rate limit exceeded. Try again later."
                        "path": %s
                        "timestamp": %s
                    }
                    """, path, now));
        }
    }

    private RatePolicy resolvePolicy(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (path.startsWith("/api/auth") || path.startsWith("/api/documents/upload")) {
            return RatePolicy.STRICT;
        }

        if ("GET".equalsIgnoreCase(method)) {
            return RatePolicy.READ;
        }

        if ("POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method)) {
            return RatePolicy.WRITE;
        }

        return null;
    }

    private String getClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
