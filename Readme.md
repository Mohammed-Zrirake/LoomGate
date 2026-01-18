# ⚡ LoomGate: High-Performance Virtual Thread Proxy

**LoomGate** is a lightweight, high-concurrency reverse proxy and load balancer written in pure Java 21+.

Unlike traditional reactive frameworks (Netty, WebFlux) that rely on complex asynchronous callbacks to handle high loads, LoomGate leverages **Project Loom (Virtual Threads)** to utilize a simple, synchronous "thread-per-request" model that scales to hundreds of thousands of concurrent connections with minimal memory footprint.

## 🚀 Key Concepts Demonstrated

This project serves as a proof-of-concept for modern Java systems programming, focusing on:

1.  **Concurrency with Project Loom:** abandoning thread pools for `Executors.newVirtualThreadPerTaskExecutor()` to handle massive I/O bound traffic.
2.  **Advanced Exception Handling:** Custom hierarchy for resilience, including `BackendUnavailableException` and retry logic.
3.  **Resilience Patterns:** A custom implementation of the **Circuit Breaker** pattern to handle backend failures gracefully.
4.  **Lambdas & Streams:** Real-time analysis of request latency and throughput using Stream API aggregations on concurrent data structures.
5.  **Structured Logging:** Asynchronous logging with SLF4J and correlation IDs for distributed tracing.

## 🛠 Architecture

### The "Blocking" Advantage
Traditionally, blocking I/O (waiting for a backend API to respond) kills performance. With Virtual Threads, the JVM unmounts the thread during I/O operations, making blocking code virtually free.

**The Flow:**
1.  **Listener:** Accepts incoming TCP connection.
2.  **Dispatch:** Spawns a new Virtual Thread instantly.
3.  **Routing:** Selects a backend server (Round Robin).
4.  **Forwarding:** Sends request via `java.net.http.HttpClient` (Blocking Mode).
5.  **Analysis:** Pushes metrics to a concurrent queue for stream processing.

## ✨ Features

- **Massive Concurrency:** Handles 10,000+ simultaneous connections on a standard laptop.
- **Load Balancing:** Rotates traffic between multiple backend instances.
- **Fault Tolerance:**
    - *Retry Mechanism:* Automatically retries failed requests with exponential backoff.
    - *Circuit Breaker:* "Trips" the circuit if error rates exceed 50%, preventing cascading failures.
- **Live Analytics:** A background task streams request history to calculate P99 Latency and Requests Per Second (RPS).

## 💻 Tech Stack

- **Language:** Java 21 (LTS) or Java 23
- **Core Networking:** `java.net.ServerSocket`, `java.net.http.HttpClient`
- **Concurrency:** Virtual Threads (`java.lang.Thread.ofVirtual()`)
- **Logging:** SLF4J + Logback
- **Build Tool:** Maven / Gradle

## 🧩 Code Snippet: Virtual Thread Dispatcher

```java
// The core engine of LoomGate
try (var serverSocket = new ServerSocket(port)) {
    logger.info("LoomGate Proxy started on port " + port);
    
    // The magic of Loom: No fixed thread pool size
    var executor = Executors.newVirtualThreadPerTaskExecutor();

    while (running) {
        var clientSocket = serverSocket.accept();
        executor.submit(() -> requestHandler.handle(clientSocket));
    }
}