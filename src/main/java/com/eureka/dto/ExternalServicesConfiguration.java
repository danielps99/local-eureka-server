package com.eureka.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;

/**
 * Root DTO for external services configuration file
 */
public record ExternalServicesConfiguration(
    /**
     * List of external services to register
     */
    List<ExternalServiceConfig> services
) {
    @JsonCreator
    public ExternalServicesConfiguration {
        // Compact constructor for validation if needed
    }
}

