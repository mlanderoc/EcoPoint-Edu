package com.ecopoint.security;

import com.ecopoint.exceptions.AccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Answers 403 with {@code ERR_FORBIDDEN} when the security filters reject a
 * request, for example a missing or invalid CSRF token (spec 003, decision T3).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    /** Message of a request rejected for its CSRF token. */
    public static final String CSRF_MESSAGE =
            "La petición no tiene un token de seguridad válido. Recargue la página e intente de nuevo.";
    private static final String FORBIDDEN_MESSAGE = "No tiene permiso para realizar esta acción.";

    private final ErrorResponseWriter writer;

    /**
     * Creates the handler.
     *
     * @param writer error body writer
     */
    public JsonAccessDeniedHandler(ErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       org.springframework.security.access.AccessDeniedException exception) throws IOException {
        String message = exception instanceof CsrfException ? CSRF_MESSAGE : FORBIDDEN_MESSAGE;
        writer.write(request, response, HttpServletResponse.SC_FORBIDDEN, AccessDeniedException.ERROR_CODE, message);
    }
}
