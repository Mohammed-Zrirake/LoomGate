#  LoomGate: High-Performance Virtual Thread Proxy

**LoomGate** is a lightweight, high-concurrency reverse proxy and load balancer written in pure Java 21+.

Unlike traditional reactive frameworks (Netty, WebFlux) that rely on complex asynchronous callbacks to handle high loads, LoomGate leverages **Project Loom (Virtual Threads)** to utilize a simple, synchronous "thread-per-request" model that scales to hundreds of thousands of concurrent connections with minimal memory footprint.

##  Key Concepts Demonstrated

This project serves as a proof-of-concept for modern Java systems programming, focusing on:

1.  **Concurrency withProject Loom:** abandoning thread pools for `Executors.newVirtualThreadPerTaskExecutor()` to handle massive I/O bound traffic.
2.  **Advanced Exception Handling:** Custom hierarchy for resilience, including `BackendUnavailableException` and retry logic.
3.  **Resilience Patterns:** A custom implementation of the **Circuit Breaker** pattern to handle backend failures gracefully.
4.  **Lambdas & Streams:** Real-time analysis of request latency and throughput using Stream API aggregations on concurrent data structures.
5.  **Structured Logging:** Asynchronous logging with SLF4J and correlation IDs for distributed tracing.

##  Architecture

### The "Blocking" Advantage
Traditionally, blocking I/O (waiting for a backend API to respond) kills performance. With Virtual Threads, the JVM unmounts the thread during I/O operations, making blocking code virtually free.

**The Flow:**
1.  **Listener:** Accepts incoming TCP connection.
2.  **Dispatch:** Spawns a new Virtual Thread instantly.
3.  **Routing:** Selects a backend server (Round Robin).
4.  **Forwarding:** Sends request via `java.net.http.HttpClient` (Blocking Mode).
5.  **Analysis:** Pushes metrics to a concurrent queue for stream processing.

##  Features

- **Massive Concurrency:** Handles 10,000+ simultaneous connections on a standard laptop.
- **Load Balancing:** Rotates traffic between multiple backend instances.
- **Fault Tolerance:**
    - *Retry Mechanism:* Automatically retries failed requests with exponential backoff.
    - *Circuit Breaker:* "Trips" the circuit if error rates exceed 50%, preventing cascading failures.
- **Live Analytics:** A background task streams request history to calculate P99 Latency and Requests Per Second (RPS).

##  Tech Stack

- **Language:** Java 21 (LTS) or Java 23
- **Core Networking:** `java.net.ServerSocket`, `java.net.http.HttpClient`
- **Concurrency:** Virtual Threads (`java.lang.Thread.ofVirtual()`)
- **Logging:** SLF4J + Logback
- **Build Tool:** Maven

##  Getting Started

### Prerequisites
- **JDK 21** (or higher) with `--enable-preview` if running on Java 21 (optional on 23+).
- **Maven** (3.9+)

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/zrirak/LoomGate.git
   ```
2. Build the project:
   ```bash
   mvn clean package
   ```

### Running the Proxy
```bash
java --enable-preview -jar target/LoomGate-1.0.0-SNAPSHOT.jar
```

## ⚙️ Configuration

LoomGate is configured via `src/main/resources/application.yml`. Key settings:

```yaml
loomgate:
  server:
    port: 8080      
  backends:
    - url: "http://localhost:8081"
      weight: 1
    - url: "http://localhost:8082" 
      weight: 1
  circuitBreaker:
    failureThreshold: 5 
```

## �📂 Project Structure & Functionality

This project is architected as a modular system where each package represents a distinct responsibility, simulating a production-grade microservice structure.

### 1. Core Networking (`com.loomgate.proxy`)
The nervous system of LoomGate.
*   **`LoomGateServer`**: The entry point that listens on the TCP port. It uses the `Executors.newVirtualThreadPerTaskExecutor()` to spawn a lightweight thread for *every* connection, making the code simple to read (synchronous) but highly scalable.
*   **`RequestHandler`**: Orchestrates the request lifecycle. It parses the incoming request, asks the Load Balancer for a target, checks the Circuit Breaker, makes the call via `BackendHttpClient`, and returns the response.
*   **`BackendHttpClient`**: A wrapper around Java's native `HttpClient`, configured for high-performance blocking I/O that plays perfectly with Virtual Threads.

### 2. Resilience Layer (`com.loomgate.circuitbreaker`)
Protects your system from death-spirals.
*   **`CircuitBreaker`**: Monitors backend failures. If a backend fails too often (e.g., 50% error rate), it "trips" the circuit, instantly rejecting new requests to that backend to give it time to recover.
*   **`RetryExecutor`**: Automatically retries failed idempotent requests with exponential backoff (wait 100ms, then 200ms, then 400ms...), smoothing out temporary network blips.

### 3. Traffic Control (`com.loomgate.loadbalancer`)
The brain deciding where traffic goes.
*   **`LoadBalancer` & `RoundRobinLoadBalancer`**: Implements the logic to distribute requests. The Round Robin strategy ensures even load distribution across all healthy backends.
*   **`BackendHealth`**: Continuously tracks which backends are UP or DOWN, ensuring the Load Balancer never sends traffic to a dead node.

### 4. Observability & Config (`com.loomgate.metrics`, `config`)
*   **`metrics`**: The "eyes" of the system. `MetricsCollector` captures atomic counters for every request, while `MetricsAnalyzer` uses Java Streams to calculate real-time P99 latency and Requests Per Second (RPS).
*   **`config`**: Maps the `application.yml` file to strongly-typed Java objects, handling type safety and validation for port settings, backend URLs, and timeout values.

### 5. Detailed File Breakdown

#### Root Directory
| File | Description |
| :--- | :--- |
| `pom.xml` | Maven configuration. Defines dependencies and the **JReleaser** plugin for automated releases. |
| `jreleaser.yml` | JReleaser config for building changelogs and publishing to GitHub. |
| `.github/workflows/release.yml` | CI/CD pipeline that automates builds and releases on push to main. |

#### Source Packages (`src/main/java/com/loomgate`)

| Package | Key Files | Functionality |
| :--- | :--- | :--- |
| `com.loomgate` | `LoomGateServer.java` | Main application bootstrap. |
| `...proxy` | `LoomGateServer`, `RequestHandler` | Handles TCP connections and request orchestration. |
| `...circuitbreaker` | `CircuitBreaker`, `RetryExecutor` | Implements fault tolerance patterns. |
| `...loadbalancer` | `RoundRobinLoadBalancer` | Distributes traffic across backends. |
| `...config` | `AppConfig`, `ProxyConfig` | Loads settings from `application.yml`. |
| `...metrics` | `MetricsCollector`, `RequestMetric` | Collects performance data (RPS, Latency). |
| `...monitoring` | `HealthCheck`, `ShutdownHook` | Manages system lifecycle and health status. |
| `...util` | `HttpUtil`, `ValidationUtil` | Helper methods for string parsing and validation. |
| `...exception` | `BackendUnavailableException` | Custom exceptions for specific failure modes. |

## 🧩 Code Snippet: Virtual Thread Dispatcher

```java

try (var serverSocket = new ServerSocket(port)) {
    logger.info("LoomGate Proxy started on port " + port);
    
    // The magic of Loom: No fixed thread pool size
    var executor = Executors.newVirtualThreadPerTaskExecutor();

    while (running) {
        var clientSocket = serverSocket.accept();
        executor.submit(() -> requestHandler.handle(clientSocket));
    }
}