package com.loomgate.proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

// Tiny Change #7: The absolute minimum working proxy
public class LoomGateServer {
    private static final Logger logger = LoggerFactory.getLogger(LoomGateServer.class);
    private volatile boolean running = true;
    private final int port;

    public LoomGateServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        try (var serverSocket = new ServerSocket(port)) {
            logger.info("LoomGate listening on port {}", port);

            // IMPORTANT: Start with 10 virtual threads, not unlimited
            var executor = Executors.newVirtualThreadPerTaskExecutor();

            while (running) {
                try {
                    var clientSocket = serverSocket.accept();

                    // Generate correlation ID for tracing
                    String correlationId = UUID.randomUUID().toString().substring(0, 8);

                    executor.submit(() -> {
                        MDC.put("correlationId", correlationId);
                        try (clientSocket) {
                            handleRequest(clientSocket);
                        } catch (IOException e) {
                            logger.error("Connection error", e);
                        } finally {
                            MDC.remove("correlationId");
                        }
                    });

                } catch (IOException e) {
                    if (running) {
                        logger.error("Accept failed", e);
                    }
                }
            }
        }
    }

    private void handleRequest(Socket clientSocket) throws IOException {
        // For now, just echo back the request
        var input = clientSocket.getInputStream();
        var output = clientSocket.getOutputStream();

        // Read first line only
        var reader = new BufferedReader(new InputStreamReader(input));
        String requestLine = reader.readLine();

        // Send back a simple response
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: 13\r\n" +
                "\r\n" +
                "Hello LoomGate!";

        output.write(response.getBytes());
        output.flush();
    }

    public void stop() {
        running = false;
    }
}