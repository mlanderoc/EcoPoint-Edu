package com.ecopoint.notification;

import com.ecopoint.model.User;
import com.ecopoint.service.PasswordResetMailer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Sends the link with the transactional email API of Brevo, over HTTPS
 * (spec D18, decision P1). The request is asynchronous, so the answer to the
 * user does not wait for the provider (decision P3).
 *
 * @author mlanderoc
 * @version 1.0
 */
public class BrevoPasswordResetMailer implements PasswordResetMailer {

    /** Endpoint of the Brevo API that sends one email. */
    public static final URI BREVO_ENDPOINT = URI.create("https://api.brevo.com/v3/smtp/email");

    private static final Logger LOG = LoggerFactory.getLogger(BrevoPasswordResetMailer.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final String SENDER_NAME = "EcoPoint Edu";

    private final HttpClient http;
    private final URI endpoint;
    private final String apiKey;
    private final String from;
    private final String frontendOrigin;
    private final ObjectMapper json = new ObjectMapper();

    /**
     * Creates the mailer.
     *
     * @param http           HTTP client
     * @param endpoint       address of the Brevo API
     * @param apiKey         API key, read from {@code ECOPOINT_MAIL_API_KEY}
     * @param from           verified sender, read from {@code ECOPOINT_MAIL_FROM}
     * @param frontendOrigin public address of the frontend, used in the link
     */
    public BrevoPasswordResetMailer(HttpClient http, URI endpoint, String apiKey, String from, String frontendOrigin) {
        this.http = http;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.from = from;
        this.frontendOrigin = frontendOrigin;
    }

    @Override
    public void sendResetLink(User user, String code) {
        ResetEmail email = ResetEmail.forUser(user, frontendOrigin, code);
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(endpoint)
                    .timeout(TIMEOUT)
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body(user, email)))
                    .build();
        } catch (JsonProcessingException e) {
            LOG.error("No se pudo preparar el correo de recuperación del usuario {}.", user.getId(), e);
            return;
        }
        http.sendAsync(request, HttpResponse.BodyHandlers.discarding()).whenComplete((response, error) -> {
            if (error != null) {
                LOG.error("No se pudo enviar el correo de recuperación del usuario {}: {}", user.getId(),
                        error.getMessage());
            } else if (response.statusCode() >= 300) {
                LOG.error("Brevo rechazó el correo de recuperación del usuario {} con el estado {}.", user.getId(),
                        response.statusCode());
            }
        });
    }

    private String body(User user, ResetEmail email) throws JsonProcessingException {
        return json.writeValueAsString(Map.of(
                "sender", Map.of("name", SENDER_NAME, "email", from),
                "to", List.of(Map.of("email", user.getEmail(), "name", user.getFirstName())),
                "subject", email.subject(),
                "textContent", email.text(),
                "htmlContent", email.html()));
    }
}
