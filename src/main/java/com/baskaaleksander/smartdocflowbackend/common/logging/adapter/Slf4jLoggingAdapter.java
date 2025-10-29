package com.baskaaleksander.smartdocflowbackend.common.logging.adapter;

import com.baskaaleksander.smartdocflowbackend.common.logging.port.LoggingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Slf4jLoggingAdapter implements LoggingPort {

    private static Logger log = LoggerFactory.getLogger(Slf4jLoggingAdapter.class);


    @Override
    public void info(String message) {
        log.info(message);
    }

    @Override
    public void warn(String message) {
        log.warn(message);
    }

    @Override
    public void error(String message, Throwable t) {
        log.error(message, t);
    }
}
