package com.baskaaleksander.smartdocflowbackend.common.exception;

public class S3DeleteException extends RuntimeException {
    public S3DeleteException(String message) {
        super(message);
    }
}
