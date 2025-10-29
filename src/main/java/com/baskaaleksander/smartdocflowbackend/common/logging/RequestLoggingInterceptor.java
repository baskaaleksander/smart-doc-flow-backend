package com.baskaaleksander.smartdocflowbackend.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME_AFTER = "reqStartTime";

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        request.setAttribute(START_TIME_AFTER, System.currentTimeMillis());

        String user = authenticatedUsernameOrAnon();
        MDC.put("user", user);

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String clientIp = clientIp(request);

        log.info("[incoming] {} {}{} from ip={} user={}",
                method, uri, query == null ? "" : "?" + query, clientIp, user
        );
        return true;

    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        Long started = (Long) request.getAttribute(START_TIME_AFTER);
        long tookMs = System.currentTimeMillis() - started;
        int status = response.getStatus();

        if (ex != null) {
            log.error("[completed] With EXCEPTION status={} took={}ms exType={} msg={}",
                    status, tookMs, ex.getClass().getSimpleName(), ex.getMessage()
            );
        } else {
            log.info("[completed] status={} took={}ms",
                    status, tookMs
            );
        }

        MDC.remove("user");
        MDC.remove("requestId");
        MDC.remove("clientIp");
    }

    private String authenticatedUsernameOrAnon() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "anonymous";
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
