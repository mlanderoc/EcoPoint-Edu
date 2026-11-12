package com.ecopoint.security;

import com.ecopoint.dto.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Writes the API error body from the security filters, which run before the
 * controllers and therefore outside {@code ApiExceptionHandler} (spec 003, R-110).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Component
public class ErrorResponseWriter {

    private final ObjectMapper json;
    private final Clock clock;

    /**
     * Creates the writer.
     *
     * @param json  JSON mapper of the application
     * @param clock clock that dates the errors
     */
    public ErrorResponseWriter(ObjectMapper json, Clock clock) {
        this.json = json;
        this.clock = clock;
    }

    /**
     * Writes an error response.
     *
     * @param request   current request
     * @param response  response to fill
     * @param status    HTTP status
     * @param errorCode error code of the API
     * @param message   message in Spanish
     * @throws IOException if the response cannot be written
     */
    public void write(HttpServletRequest request, HttpServletResponse response, int status, String errorCode,
                      String message) throws IOException {
        ApiError body = new ApiError(LocalDateTime.now(clock), status, errorCode, message, request.getRequestURI(),
                List.of());
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(response.getOutputStream(), body);
    }
}
