package com.ecopoint.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides the clock used by services and the API to get the current date and
 * time, so tests can fix it (spec 003, decision T12).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Configuration
public class ClockConfig {

    /**
     * System clock in the default time zone of the server.
     *
     * @return the application clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
