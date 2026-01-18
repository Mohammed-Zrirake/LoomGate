import java.time.Duration;
import java.util.Objects;

// Tiny Change #4: Configuration POJOs (one at a time)/ Validation

public BackendConfig {
        Objects.requireNonNull(url);
        if (weight <= 0) throw new IllegalArgumentException("Weight must be positive");
        if (maxConnections <= 0) throw new IllegalArgumentException("Max connections must be positive");
    }

// Build more config records incrementally:
// 1. CircuitBreakerConfig
// 2. ProxyConfig (main configuration)
// 3. MetricsConfig
// 4. RetryConfig