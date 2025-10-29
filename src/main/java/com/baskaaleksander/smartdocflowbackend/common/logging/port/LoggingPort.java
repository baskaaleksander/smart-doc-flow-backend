package com.baskaaleksander.smartdocflowbackend.common.logging.port;

public interface LoggingPort {
    void info(String message);

    void warn(String message);

    void error(String message, Throwable t);
}
