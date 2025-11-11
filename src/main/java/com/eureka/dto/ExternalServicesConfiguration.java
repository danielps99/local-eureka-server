package com.eureka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Root DTO for external services configuration file
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalServicesConfiguration {
    
    /**
     * List of external services to register
     */
    private List<ExternalServiceConfig> services;
}

