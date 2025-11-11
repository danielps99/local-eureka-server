package com.eureka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single external service configuration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalServiceConfig {
    
    /**
     * Service name (e.g., "hub-user-data")
     */
    private String serviceName;
    
    /**
     * External URL (e.g., "https://api-dev.bdws.com.br/hub-user-data")
     */
    private String externalUrl;
    
    /**
     * Port number (optional, will be auto-detected from URL if not provided)
     */
    private Integer port;
    
    /**
     * Whether the connection is secure (HTTPS)
     * Optional, will be auto-detected from URL scheme if not provided
     */
    private Boolean secure;
    
    /**
     * Instance ID (optional, will be auto-generated if not provided)
     */
    private String instanceId;
}

