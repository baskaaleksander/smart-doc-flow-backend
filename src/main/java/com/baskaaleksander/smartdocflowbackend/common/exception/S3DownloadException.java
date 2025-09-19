package com.baskaaleksander.smartdocflowbackend.common.exception;

public class S3DownloadException extends RuntimeException {
    public S3DownloadException(String message) {
        super(message);
    }
}
