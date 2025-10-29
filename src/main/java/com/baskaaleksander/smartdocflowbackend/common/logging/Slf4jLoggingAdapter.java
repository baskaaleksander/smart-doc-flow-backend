package com.baskaaleksander.smartdocflowbackend.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class Slf4jLoggingAdapter implements LoggingPort {

    private static Logger log = LoggerFactory.getLogger(Slf4jLoggingAdapter.class);


    @Override
    public void info(String message) {
        log.info(withContext(message));
    }

    @Override
    public void warn(String message) {
        log.warn(withContext(message));
    }

    @Override
    public void error(String message, Throwable t) {
        log.error(withContext(message), t);
    }

    @Override
    public void error(String message) {
        log.error(withContext(message));
    }

    @Override
    public void debug(String message) {
        log.debug(withContext(message));
    }

    private String withContext(String message) {
        String requestId = MDC.get("requestId");
        String user = MDC.get("user");
        String clientIp = MDC.get("clientIp");
        String module = MDC.get("module");

        StringBuilder prefix = new StringBuilder();
        if (module != null) prefix.append("[").append(module).append("] ");
        if (user != null) prefix.append("[user=").append(user).append("] ");
        if (requestId != null) prefix.append("[reqId=").append(requestId).append("] ");
        if (clientIp != null) prefix.append("[ip=").append(clientIp).append("] ");

        return prefix + message;
    }
}
