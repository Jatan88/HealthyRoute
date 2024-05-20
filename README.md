# HealthyRoute

HealthyRoute is a Java-based application layer load balancer that efficiently distributes client requests across multiple backend servers while ensuring high availability and reliability.

## Features

- **Round-robin Load Balancing**: Distributes incoming client requests evenly among available backend servers using a round-robin scheduling algorithm.
- **Health Checks**: Periodically checks the health status of backend servers and avoids sending requests to unhealthy servers.
- **Concurrent Handling**: Handles multiple client connections concurrently using multi-threading.
- **Dynamic Configuration**: Allows adding or removing backend servers dynamically without interrupting the load balancing operation.
- **Scalability**: Can scale to accommodate many backend servers and handle high volumes of client traffic.

## Getting Started

Follow these steps to set up and run the HealthyRoute on your local machine:

1. **Clone the Repository**: 
   ```bash
   git clone https://github.com/yourusername/HealthyRoute.git
   
2. **Compile the Code**:
   ```bash
   javac LoadBalancer.java

3. **Run the Load Balancer**:
   ```bash
   java LoadBalancer

## Configuration
You can configure the load balancer by modifying the backend Servers list in the LoadBalancer.java file to add or remove backend servers. 
Additionally, you can adjust the health check interval and timeout duration in the HealthCheckTask class.

## Contributing
Contributions are welcome! If you have suggestions for improvements or new features, feel free to open an issue or submit a pull request.
