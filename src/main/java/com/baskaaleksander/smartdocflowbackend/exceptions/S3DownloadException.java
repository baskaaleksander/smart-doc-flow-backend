package com.baskaaleksander.smartdocflowbackend.exceptions;

public class S3DownloadException extends RuntimeException {
    public S3DownloadException(String message) {
        super(message);
    }
}
