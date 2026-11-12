package com.ecopoint.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Body of every error response of the API (spec 003, R-110).
 *
 * @param timestamp   moment of the error
 * @param status      HTTP status code
 * @param errorCode   stable error code, such as {@code ERR_INVALID_DATA}
 * @param message     message for the user, in Spanish
 * @param path        requested path
 * @param fieldErrors invalid fields, empty when the error is not about a field
 * @author craulgomez
 * @version 1.0
 */
public record ApiError(LocalDateTime timestamp, int status, String errorCode, String message, String path,
                       List<FieldErrorDto> fieldErrors) {

    /**
     * Creates the error with an immutable copy of the field errors.
     */
    public ApiError {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }
}
