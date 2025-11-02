package com.baskaaleksander.smartdocflowbackend.common.exception;

public class EmbeddingTaskFailedException extends RuntimeException {
    public EmbeddingTaskFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
