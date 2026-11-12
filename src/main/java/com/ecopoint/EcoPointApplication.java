package com.ecopoint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Entry point of EcoPoint Edu: starts the REST API (spec 003) on top of the
 * PostgreSQL data access layer (spec 002). The default in-memory user of Spring
 * Boot is turned off, because users are checked against the database by
 * {@code AuthService}.
 *
 * @author mlanderoc
 * @version 1.0
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
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
