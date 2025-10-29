package com.baskaaleksander.smartdocflowbackend.common.logging;

public interface LoggingPort {
    void info(String message);

    void warn(String message);

    void error(String message, Throwable t);

    void error(String message);

    void debug(String message);
}
