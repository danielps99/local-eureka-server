# Local Eureka Server

A lightweight Spring Boot Eureka Server for local development and testing.

## What is Eureka?

Eureka is a service registry that allows microservices to register themselves and discover other services. This server acts as the central registry where all your services can register and find each other.

## Prerequisites

- Java 21+ (for local development)
- Maven 3.6+
- Docker (optional, for containerized deployment)

## Quick Start

### Run Locally

1. **Build the application:**
   ```bash
   mvn clean package
   ```

2. **Run the server:**
   ```bash
   java -jar target/local-eureka-server-1.0.0.jar
   ```

3. **Access the Eureka Dashboard:**
   Open your browser and navigate to: `http://localhost:8761`

### Run in Docker Container on Port 2181

1. **Build the Docker image:**
   ```bash
   docker build --network=host -t local-eureka-server .
   ```
   
   **Important:** The `--network=host` flag is **required** for building. It allows the Docker container to use the host's network stack, which resolves DNS issues during Maven dependency downloads. Without this flag, the build will fail with DNS resolution errors.

2. **Run the container (mapping to port 2181):**
   ```bash
   docker run -p 2181:8761 local-eureka-server
   ```

3. **Access the Eureka Dashboard:**
   Open your browser and navigate to: `http://localhost:2181`

## Configuration

The server is configured to run on port `8761` by default. The configuration is in `src/main/resources/application.yml`.

To change the port, modify the `server.port` property in `application.yml`.

## External Services Registration

This Eureka server supports automatic registration of external services (services not running locally) from a JSON configuration file. This is useful for local development when you need to connect to external APIs.

### Configuration File

External services are configured in `src/main/resources/external-services.json`. The file format is:

```json
{
  "services": [
    {
      "serviceName": "hub-user-data",
      "externalUrl": "https://api-dev.bdws.com.br/hub-user-data",
      "port": 443,
      "secure": true,
      "instanceId": null
    }
  ]
}
```

**Fields:**
- `serviceName` (required): The service name to register in Eureka
- `externalUrl` (required): The full external URL of the service
- `port` (optional): Port number (auto-detected from URL if not provided)
- `secure` (optional): Whether to use HTTPS (auto-detected from URL if not provided)
- `instanceId` (optional): Custom instance ID (auto-generated if not provided)

### How It Works

1. On startup, the server reads `external-services.json`
2. Each service in the file is automatically registered with Eureka
3. Heartbeats are sent every 25 seconds to keep services registered
4. Services remain registered as long as the server is running

### Custom Configuration File Location

You can specify a custom location for the configuration file in `application.yml`:

```yaml
eureka:
  external-services:
    config-file: classpath:external-services.json  # or file:/path/to/file.json
```

### Example: Adding a New External Service

1. Edit `src/main/resources/external-services.json`
2. Add a new service entry:
   ```json
   {
     "serviceName": "my-external-service",
     "externalUrl": "https://api.example.com/my-service",
     "port": 443,
     "secure": true
   }
   ```
3. Restart the Eureka server
4. The service will be automatically registered

### Using Registered Services in Client Applications

#### Required Client Configuration

**CRITICAL:** Your client application **must** have the following configuration in `application.yml`:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    fetch-registry: true  # REQUIRED: Must be true to fetch services
    registry-fetch-interval-seconds: 5  # Refresh every 5 seconds

spring:
  cloud:
    loadbalancer:
      cache:
        enabled: false  # Disable cache for fresh lookups
        ttl: 0
```

**Important:** Without `fetch-registry: true`, your client will NOT discover services from Eureka!

#### Feign Client Configuration

When using Feign clients to call external services with base paths, you must specify the path in the `@FeignClient` annotation:

```java
@FeignClient(value = "hub-user-data", path = "/hub-user-data")
public interface IHubN4Client {
    // ... methods
}
```

This is because Eureka only stores hostname and port, not the base path. The base path must be configured in the Feign client.

#### Troubleshooting

If you see errors like "Load balancer does not have available server for client: hub-user-data", see [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for detailed solutions.

## Docker Image Details

- **Build Image:** `maven:3.9-eclipse-temurin-25-alpine` (builds with Java 25)
- **Runtime Image:** `eclipse-temurin:25-jre-alpine` (runs on Java 25, lightweight Alpine Linux)
- **Size:** ~150-200MB (optimized with multi-stage build)
- **Port:** 8761 (internal), mapped to 2181 on host when using the example command
- **Note:** The application compiles to Java 21 bytecode for Spring Boot compatibility but runs on Java 25 runtime (backward compatible)

## Usage with Other Services

To register your Spring Boot microservices with this Eureka server:

1. Add the Eureka client dependency to your service
2. Configure the Eureka server URL in your service's `application.yml`:
   ```yaml
   eureka:
     client:
       service-url:
         defaultZone: http://localhost:8761/eureka/
   ```
   (Use `http://localhost:2181/eureka/` if running in Docker on port 2181)

## Notes

- This is a standalone Eureka server (doesn't register with other Eureka instances)
- Suitable for local development and testing
- For production, consider setting up a cluster of Eureka servers for high availability

