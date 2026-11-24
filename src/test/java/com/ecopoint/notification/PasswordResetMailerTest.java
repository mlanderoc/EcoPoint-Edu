package com.ecopoint.notification;

import com.ecopoint.config.MailConfig;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.service.PasswordResetMailer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the email with the recovery link and of the choice of mailer
 * (spec D18, RP-09 to RP-11).
 *
 * @author mlanderoc
 * @version 1.0
 */
class PasswordResetMailerTest {

    private static final String ORIGIN = "https://ecopoint.example";
    private final User student = new User(11L, "Laura", "Pérez", "laura@unicesar.edu.co",
            UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void rp09_emailIsInSpanishWithTheLinkAndNoPassword() {
        ResetEmail email = ResetEmail.forUser(student, ORIGIN + "/", "abc_123-XYZ");

        String link = ORIGIN + "/restablecer-contrasena?token=abc_123-XYZ";
        assertTrue(email.text().contains(link));
        assertTrue(email.html().contains(link));
        assertTrue(email.text().contains("vence en 30 minutos"));
        assertTrue(email.text().contains("Si no lo pediste"));
        assertFalse(email.text().toLowerCase().contains("contraseña actual"));
    }

    @Test
    void rp09_nameIsEscapedInTheHtml() {
        User odd = new User(12L, "<b>Ana</b>", "Gil", "ana@unicesar.edu.co", UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);

        assertFalse(ResetEmail.forUser(odd, ORIGIN, "x").html().contains("<b>Ana</b>"));
    }

    @Test
    void p1_brevoReceivesTheEmailWithTheKeyInTheHeader() throws Exception {
        BlockingQueue<String[]> received = new ArrayBlockingQueue<>(1);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/smtp/email", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            received.add(new String[] {exchange.getRequestHeaders().getFirst("api-key"), body});
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v3/smtp/email");
        PasswordResetMailer mailer = new BrevoPasswordResetMailer(HttpClient.newHttpClient(), endpoint,
                "clave-de-prueba", "ecopoint.demo@example.com", ORIGIN);

        mailer.sendResetLink(student, "codigo123");

        String[] request = received.poll(10, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("clave-de-prueba", request[0]);
        JsonNode json = new ObjectMapper().readTree(request[1]);
        assertEquals("ecopoint.demo@example.com", json.at("/sender/email").asText());
        assertEquals("laura@unicesar.edu.co", json.at("/to/0/email").asText());
        assertTrue(json.at("/textContent").asText().contains("token=codigo123"));
    }

    @Test
    void p3_failureOfBrevoDoesNotReachTheUser() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");

        new BrevoPasswordResetMailer(HttpClient.newHttpClient(), endpoint, "k", "a@example.com", ORIGIN)
                .sendResetLink(student, "codigo123");
    }

    @Test
    void rp11_mailerDependsOnTheConfigurationAndTheProfile() {
        MailConfig config = new MailConfig();
        MockEnvironment local = new MockEnvironment();
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");

        assertInstanceOf(ConsolePasswordResetMailer.class, config.passwordResetMailer(local, "", "", ORIGIN));
        assertInstanceOf(DisabledPasswordResetMailer.class, config.passwordResetMailer(prod, "", "", ORIGIN));
        assertInstanceOf(DisabledPasswordResetMailer.class, config.passwordResetMailer(prod, "clave", " ", ORIGIN));
        assertInstanceOf(BrevoPasswordResetMailer.class,
                config.passwordResetMailer(prod, "clave", "a@example.com", ORIGIN));
    }
}
