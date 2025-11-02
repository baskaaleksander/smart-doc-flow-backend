package com.baskaaleksander.smartdocflowbackend.common.exception;

public class OcrTaskFailedException extends RuntimeException {
    public OcrTaskFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
