
package com.loomgate.exception;

import org.slf4j.MDC;

import java.time.Duration;
import java.time.Instant;

public class LoomGateException extends RuntimeException {
    private final Instant timestamp;
    private final String correlationId;

    public LoomGateException(String message, Throwable cause) {
        super(message, cause);
        this.timestamp = Instant.now();
        this.correlationId = MDC.get("correlationId");
    }

    // Add getters
}


public class BackendUnavailableException extends LoomGateException {
    private final String backendUrl;
    // Constructor with backend URL
}

public class CircuitBreakerOpenException extends LoomGateException {
    private final String circuitName;
    private final Duration remainingTimeout;
    // Constructor
}

public class RequestTimeoutException extends LoomGateException {
    private final Duration timeout;
    private final String requestId;
    // Constructor
}

