package com.eureka.service;

import com.eureka.dto.ExternalServiceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service to register external services with Eureka
 */
@Slf4j
@Service
public class ServiceRegistrationService {

    private final HttpClient httpClient;
    
    @Value("${eureka.client.service-url.defaultZone:http://localhost:8761/eureka/}")
    private String eurekaUrl;
    
    @Autowired(required = false)
    private ServiceHeartbeatService heartbeatService;

    public ServiceRegistrationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Registers a service with external URL in Eureka
     */
    public boolean registerService(ExternalServiceConfig config) {
        try {
            URI externalUri = URI.create(config.getExternalUrl());
            String host = externalUri.getHost();
            String path = externalUri.getPath();
            // Remove trailing slash from path
            if (path != null && path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }
            int port = config.getPort() != null ? config.getPort() : 
                       (externalUri.getScheme().equals("https") ? 443 : 80);
            boolean secure = config.getSecure() != null ? config.getSecure() : 
                           externalUri.getScheme().equals("https");
            
            String serviceName = config.getServiceName().toUpperCase();
            String instanceId = config.getInstanceId() != null ? 
                               config.getInstanceId() : 
                               host + ":" + config.getServiceName() + ":" + port;
            
            long timestamp = System.currentTimeMillis();
            
            // Build Eureka registration JSON
            String jsonPayload = buildEurekaRegistrationJson(
                    serviceName, instanceId, host, port, secure, 
                    config.getExternalUrl(), path, timestamp
            );
            
            String eurekaEndpoint = eurekaUrl.replace("/eureka/", "") + "/eureka/apps/" + serviceName;
            
            log.info("Registering service {} -> {} with Eureka at {}", 
                    serviceName, config.getExternalUrl(), eurekaEndpoint);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(eurekaEndpoint))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, 
                    HttpResponse.BodyHandlers.ofString());
            
            int statusCode = response.statusCode();
            if (statusCode == 204 || statusCode == 200) {
                log.info("Successfully registered service {} with Eureka", serviceName);
                // Register for automatic heartbeat renewal
                if (heartbeatService != null) {
                    heartbeatService.registerServiceForHeartbeat(serviceName, instanceId);
                }
                return true;
            } else {
                log.error("Failed to register service. Status: {}, Response: {}", 
                        statusCode, response.body());
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error registering service: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private String buildEurekaRegistrationJson(String serviceName, String instanceId, 
                                                String host, int port, boolean secure,
                                                String externalUrl, String basePath, long timestamp) {
        try {
            // Build the JSON structure that Eureka expects
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"instance\": {\n");
            json.append("    \"instanceId\": \"").append(instanceId).append("\",\n");
            json.append("    \"hostName\": \"").append(host).append("\",\n");
            json.append("    \"app\": \"").append(serviceName).append("\",\n");
            json.append("    \"ipAddr\": \"").append(host).append("\",\n");
            json.append("    \"status\": \"UP\",\n");
            json.append("    \"overriddenStatus\": \"UNKNOWN\",\n");
            json.append("    \"port\": {\n");
            json.append("      \"$\": ").append(port).append(",\n");
            json.append("      \"@enabled\": ").append(!secure).append("\n");
            json.append("    },\n");
            json.append("    \"securePort\": {\n");
            json.append("      \"$\": ").append(port).append(",\n");
            json.append("      \"@enabled\": ").append(secure).append("\n");
            json.append("    },\n");
            json.append("    \"homePageUrl\": \"").append(externalUrl).append("\",\n");
            json.append("    \"statusPageUrl\": \"").append(externalUrl).append("/actuator/info\",\n");
            json.append("    \"healthCheckUrl\": \"").append(externalUrl).append("/actuator/health\",\n");
            json.append("    \"healthCheckUrlPath\": \"").append(externalUrl).append("/actuator/health\",\n");
            json.append("    \"vipAddress\": \"").append(serviceName.toLowerCase()).append("\",\n");
            json.append("    \"secureVipAddress\": \"").append(serviceName.toLowerCase()).append("\",\n");
            // Add base path in metadata so clients know about it
            if (basePath != null && !basePath.isEmpty()) {
                json.append("    \"metadata\": {\n");
                json.append("      \"management.port\": \"").append(port).append("\",\n");
                json.append("      \"external.url\": \"").append(externalUrl).append("\",\n");
                json.append("      \"base.path\": \"").append(basePath).append("\"\n");
                json.append("    },\n");
            } else {
                json.append("    \"metadata\": {\n");
                json.append("      \"management.port\": \"").append(port).append("\",\n");
                json.append("      \"external.url\": \"").append(externalUrl).append("\"\n");
                json.append("    },\n");
            }
            json.append("    \"dataCenterInfo\": {\n");
            json.append("      \"@class\": \"com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo\",\n");
            json.append("      \"name\": \"MyOwn\"\n");
            json.append("    },\n");
            json.append("    \"leaseInfo\": {\n");
            json.append("      \"renewalIntervalInSecs\": 30,\n");
            json.append("      \"durationInSecs\": 90,\n");
            json.append("      \"registrationTimestamp\": ").append(timestamp).append(",\n");
            json.append("      \"lastRenewalTimestamp\": ").append(timestamp).append(",\n");
            json.append("      \"evictionTimestamp\": 0,\n");
            json.append("      \"serviceUpTimestamp\": ").append(timestamp).append("\n");
            json.append("    },\n");
            json.append("    \"isCoordinatingDiscoveryServer\": false,\n");
            json.append("    \"lastUpdatedTimestamp\": \"").append(timestamp).append("\",\n");
            json.append("    \"lastDirtyTimestamp\": \"").append(timestamp).append("\",\n");
            json.append("    \"actionType\": \"ADDED\"\n");
            json.append("  }\n");
            json.append("}");
            return json.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Eureka registration JSON", e);
        }
    }
}

