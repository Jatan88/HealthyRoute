import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LoadBalancer {

    private static final List<String> backendServers = new ArrayList<>();
    private static final List<Boolean> serverStatus = new ArrayList<>();
    private static int currentServerIndex = 0;
    private static final Lock lock = new ReentrantLock();

    public static void main(String[] args) {
        // Add backend servers (IP:Port)
        backendServers.add("localhost:8082");
        backendServers.add("localhost:8083");
        backendServers.add("localhost:8084");

        // Initialize server status (all servers start as healthy)
        for (String backendServer : backendServers) {
            serverStatus.add(true);
        }

        // Start periodic health checks
        Timer healthCheckTimer = new Timer();
        healthCheckTimer.schedule(new HealthCheckTask(), 0, 10000); // Health check every 10 seconds

        int port = 8083;
        try {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Load balancer running on port " + port);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Received request from " + clientSocket.getInetAddress());

                    // Handle client request in a new thread
                    new Thread(() -> handleRequest(clientSocket)).start();
                }
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private static void handleRequest(Socket clientSocket) {
        try {
            try (clientSocket) {
                InputStream inputStream = clientSocket.getInputStream();
                OutputStream outputStream = clientSocket.getOutputStream();
                // Read the request from the client
                byte[] buffer = new byte[1024];
                int bytesRead = inputStream.read(buffer);
                String request = new String(buffer, 0, bytesRead);
                System.out.println("Request from client:\n" + request);
                // Forward the request to a healthy backend server using round-robin scheduling
                forwardRequestToBackend(request, outputStream);
                // Close the client connection
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private static void forwardRequestToBackend(String request, OutputStream clientOutputStream) {
        String backendServer;
        lock.lock();
        try {
            // Find the next healthy backend server using round-robin scheduling
            int startIndex = currentServerIndex;
            do {
                backendServer = backendServers.get(currentServerIndex);
                currentServerIndex = (currentServerIndex + 1) % backendServers.size();
                if (serverStatus.get(currentServerIndex)) {
                    break; // Found a healthy server
                }
            } while (currentServerIndex != startIndex);

            if (!serverStatus.get(currentServerIndex)) {
                System.out.println("All backend servers are unhealthy. Unable to forward request.");
                return;
            }
        } finally {
            lock.unlock();
        }

        String[] serverInfo = backendServer.split(":");
        String backendHost = serverInfo[0];
        int backendPort = Integer.parseInt(serverInfo[1]);

        try (Socket backendSocket = new Socket(backendHost, backendPort)) {
            // Send the request to the backend server
            OutputStream backendOutputStream = backendSocket.getOutputStream();
            backendOutputStream.write(request.getBytes());
            backendOutputStream.flush();

            // Get the response from the backend server
            InputStream backendInputStream = backendSocket.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead = backendInputStream.read(buffer);
            String response = new String(buffer, 0, bytesRead);

            // Send the response back to the client
            clientOutputStream.write(response.getBytes());
            clientOutputStream.flush();
            System.out.println("Response from backend server (" + backendHost + ":" + backendPort + "):\n" + response);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private static class HealthCheckTask extends TimerTask {
        @Override
        public void run() {
            for (int i = 0; i < backendServers.size(); i++) {
                String server = backendServers.get(i);
                boolean isHealthy = checkServerHealth(server);
                lock.lock();
                try {
                    serverStatus.set(i, isHealthy);
                } finally {
                    lock.unlock();
                }
            }
        }

        private boolean checkServerHealth(String server) {
            String healthCheckUrl = "http://" + server + "/health-check";
            try {
                URL url = new URL(healthCheckUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(2000); // 2 seconds timeout for health check
                int responseCode = connection.getResponseCode();
                return (responseCode == 200);
            } catch (IOException e) {
                return false; // Health check failed
            }
        }
    }
}
