package com.baskaaleksander.smartdocflowbackend.exceptions;

public class PdfProcessingException extends RuntimeException {
    public PdfProcessingException(String message) {
        super(message);
    }
}
