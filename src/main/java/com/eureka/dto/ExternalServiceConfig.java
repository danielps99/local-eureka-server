package com.eureka.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * DTO representing a single external service configuration
 */
public record ExternalServiceConfig(
    /**
     * Service name (e.g., "hub-user-data")
     */
    String serviceName,
    
    /**
     * External URL (e.g., "https://api-dev.bdws.com.br/hub-user-data")
     */
    String externalUrl,
    
    /**
     * Port number (optional, will be auto-detected from URL if not provided)
     */
    Integer port,
    
    /**
     * Whether the connection is secure (HTTPS)
     * Optional, will be auto-detected from URL scheme if not provided
     */
    Boolean secure,
    
    /**
     * Instance ID (optional, will be auto-generated if not provided)
     */
    String instanceId
) {
    @JsonCreator
    public ExternalServiceConfig {
        // Compact constructor for validation if needed
    }
}

