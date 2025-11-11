package com.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Local Eureka Server with file-based external service registration.
 * 
 * External services are automatically registered on startup from:
 * src/main/resources/external-services.json
 */
@SpringBootApplication
@EnableEurekaServer
@EnableScheduling
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}

