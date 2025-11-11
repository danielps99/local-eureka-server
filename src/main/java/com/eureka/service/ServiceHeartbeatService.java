package com.eureka.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to automatically renew leases for manually registered services
 */
@Service
public class ServiceHeartbeatService {
    
    private static final Logger log = LoggerFactory.getLogger(ServiceHeartbeatService.class);

    @Value("${eureka.client.service-url.defaultZone:http://localhost:8761/eureka/}")
    private String eurekaUrl;
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    
    // Track registered services that need heartbeats
    private final Map<String, ServiceInfo> registeredServices = new ConcurrentHashMap<>();
    
    public void registerServiceForHeartbeat(String serviceName, String instanceId) {
        registeredServices.put(serviceName, new ServiceInfo(serviceName, instanceId));
        log.info("Registered {} for automatic heartbeat renewal", serviceName);
    }
    
    /**
     * Renew lease every 25 seconds (before 30 second default interval)
     */
    @Scheduled(fixedRate = 25000)
    public void renewLeases() {
        if (registeredServices.isEmpty()) {
            return;
        }
        
        for (ServiceInfo serviceInfo : registeredServices.values()) {
            try {
                renewLease(serviceInfo.serviceName, serviceInfo.instanceId);
            } catch (Exception e) {
                log.warn("Failed to renew lease for {}: {}", serviceInfo.serviceName, e.getMessage());
            }
        }
    }
    
    private void renewLease(String serviceName, String instanceId) {
        try {
            String renewUrl = eurekaUrl.replace("/eureka/", "") + 
                            "/eureka/apps/" + serviceName.toUpperCase() + "/" + instanceId;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(renewUrl))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(5))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                log.debug("Successfully renewed lease for {}", serviceName);
            } else if (response.statusCode() == 404) {
                log.warn("Service {} not found in Eureka, may need re-registration", serviceName);
                // Optionally re-register here
            }
        } catch (Exception e) {
            log.error("Error renewing lease for {}: {}", serviceName, e.getMessage());
        }
    }
    
    private static class ServiceInfo {
        final String serviceName;
        final String instanceId;
        
        ServiceInfo(String serviceName, String instanceId) {
            this.serviceName = serviceName;
            this.instanceId = instanceId;
        }
    }
}

