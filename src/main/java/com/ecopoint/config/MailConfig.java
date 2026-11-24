package com.ecopoint.config;

import com.ecopoint.notification.BrevoPasswordResetMailer;
import com.ecopoint.notification.ConsolePasswordResetMailer;
import com.ecopoint.notification.DisabledPasswordResetMailer;
import com.ecopoint.service.PasswordResetMailer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Chooses how the password recovery link is sent (spec D18, RP-10 and RP-11):
 * by Brevo when its key and sender are configured; otherwise in the console on
 * a local computer, and nowhere in production.
 *
 * @author mlanderoc
 * @version 1.0
 */
@Configuration
public class MailConfig {

    /**
     * Mailer of the recovery link.
     *
     * @param environment    active profiles
     * @param apiKey         Brevo API key, from {@code ECOPOINT_MAIL_API_KEY}
     * @param from           verified sender, from {@code ECOPOINT_MAIL_FROM}
     * @param frontendOrigin public address of the frontend
     * @return the mailer
     */
    @Bean
    public PasswordResetMailer passwordResetMailer(Environment environment,
                                                   @Value("${ecopoint.mail.api-key:}") String apiKey,
                                                   @Value("${ecopoint.mail.from:}") String from,
                                                   @Value("${ecopoint.frontend-origin}") String frontendOrigin) {
        if (!apiKey.isBlank() && !from.isBlank()) {
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            return new BrevoPasswordResetMailer(http, BrevoPasswordResetMailer.BREVO_ENDPOINT, apiKey.trim(),
                    from.trim(), frontendOrigin);
        }
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            return new DisabledPasswordResetMailer();
        }
        return new ConsolePasswordResetMailer(frontendOrigin);
    }
}
