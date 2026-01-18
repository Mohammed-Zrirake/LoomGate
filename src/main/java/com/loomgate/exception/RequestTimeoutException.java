package com.loomgate.exception;

public class RequestTimeoutException extends RuntimeException {
    public RequestTimeoutException(String message) {
        super(message);
    }
}
