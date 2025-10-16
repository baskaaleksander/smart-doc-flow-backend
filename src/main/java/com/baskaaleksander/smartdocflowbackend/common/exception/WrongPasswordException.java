package com.baskaaleksander.smartdocflowbackend.common.exception;

public class WrongPasswordException extends RuntimeException {
    public WrongPasswordException(String message) {
        super(message);
    }
}
