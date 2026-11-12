package com.ecopoint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of EcoPoint Edu. In spec 002 it only starts the data access
 * layer (PostgreSQL connection and repositories); the REST API is added in
 * spec 003.
 *
 * @author mlanderoc
 * @version 1.0
 */
@SpringBootApplication
public class EcoPointApplication {

    /**
     * Starts the application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(EcoPointApplication.class, args);
    }
}
