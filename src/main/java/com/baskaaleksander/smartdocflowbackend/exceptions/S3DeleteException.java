package com.baskaaleksander.smartdocflowbackend.exceptions;

public class S3DeleteException extends RuntimeException {
    public S3DeleteException(String message) {
        super(message);
    }
}
