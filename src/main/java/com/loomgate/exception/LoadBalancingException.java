package com.loomgate.exception;

public class LoadBalancingException extends RuntimeException {
    public LoadBalancingException(String message) {
        super(message);
    }
}
