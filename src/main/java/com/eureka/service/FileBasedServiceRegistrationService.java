package com.eureka.service;

import com.eureka.dto.ExternalServiceConfig;
import com.eureka.dto.ExternalServicesConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 * Service that reads external services configuration from JSON file
 * and registers them with Eureka on application startup
 */
@Slf4j
@Service
public class FileBasedServiceRegistrationService implements CommandLineRunner {

    private final ResourceLoader resourceLoader;
    private final ServiceRegistrationService registrationService;
    private final ObjectMapper objectMapper;
    
    @Value("${eureka.external-services.config-file:classpath:external-services.json}")
    private String configFileLocation;

    public FileBasedServiceRegistrationService(
            ResourceLoader resourceLoader,
            ServiceRegistrationService registrationService) {
        this.resourceLoader = resourceLoader;
        this.registrationService = registrationService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Loading external services configuration from: {}", configFileLocation);
        
        try {
            Resource resource = resourceLoader.getResource(configFileLocation);
            
            if (!resource.exists()) {
                log.warn("External services configuration file not found: {}. Skipping external service registration.", 
                        configFileLocation);
                return;
            }
            
            ExternalServicesConfiguration config;
            try (InputStream inputStream = resource.getInputStream()) {
                config = objectMapper.readValue(inputStream, ExternalServicesConfiguration.class);
            }
            
            if (config == null || config.getServices() == null || config.getServices().isEmpty()) {
                log.info("No external services configured in {}", configFileLocation);
                return;
            }
            
            log.info("Found {} external service(s) to register", config.getServices().size());
            
            // Wait a bit for Eureka server to be fully initialized
            Thread.sleep(2000);
            
            List<ExternalServiceConfig> services = config.getServices();
            int successCount = 0;
            int failureCount = 0;
            
            for (ExternalServiceConfig serviceConfig : services) {
                if (serviceConfig.getServiceName() == null || serviceConfig.getServiceName().trim().isEmpty()) {
                    log.warn("Skipping service with empty service name");
                    failureCount++;
                    continue;
                }
                
                if (serviceConfig.getExternalUrl() == null || serviceConfig.getExternalUrl().trim().isEmpty()) {
                    log.warn("Skipping service {} with empty external URL", serviceConfig.getServiceName());
                    failureCount++;
                    continue;
                }
                
                boolean success = registrationService.registerService(serviceConfig);
                if (success) {
                    successCount++;
                } else {
                    failureCount++;
                }
                
                // Small delay between registrations
                Thread.sleep(500);
            }
            
            log.info("External service registration completed. Success: {}, Failed: {}", 
                    successCount, failureCount);
            
        } catch (Exception e) {
            log.error("Error loading or registering external services from {}: {}", 
                    configFileLocation, e.getMessage(), e);
        }
    }
}

