package com.ecopoint.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Answers 401 with {@code ERR_UNAUTHORIZED} when a request has no session or the
 * session expired (spec 003, R-121 and CA-200).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /** Error code of a request without a valid session. */
    public static final String ERR_UNAUTHORIZED = "ERR_UNAUTHORIZED";
    /** Message of a request without a valid session. */
    public static final String MESSAGE = "Debe iniciar sesión para continuar.";

    private final ErrorResponseWriter writer;

    /**
     * Creates the entry point.
     *
     * @param writer error body writer
     */
    public JsonAuthenticationEntryPoint(ErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        writer.write(request, response, HttpServletResponse.SC_UNAUTHORIZED, ERR_UNAUTHORIZED, MESSAGE);
    }
}
