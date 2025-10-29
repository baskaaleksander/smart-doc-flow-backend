package com.baskaaleksander.smartdocflowbackend.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class MDCRequestFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        String module = detectModule(request.getRequestURI());
        MDC.put("requestId", requestId);
        MDC.put("clientIp", clientIp(request));
        MDC.put("module", module);

        try {
            response.setHeader("X-Request-ID", requestId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
            MDC.remove("clientIp");
            MDC.remove("module");
        }
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private String detectModule(String uri) {
        if (uri.startsWith("/api/auth")) return "auth";
        if (uri.startsWith("/api/users")) return "users";
        if (uri.startsWith("/api/notifications")) return "notifications";
        if (uri.startsWith("/api/reviews")) return "reviews";
        if (uri.endsWith("conversations")) return "conversations";
        if (uri.startsWith("/api/documents")) return "documents";

        return "core";
    }
}
