package com.ecopoint.exceptions;

/**
 * Exception thrown when the user cannot be identified: wrong email or password,
 * inactive or temporarily locked account, or a session that is no longer valid
 * (spec 003, R-121, CA-202 and R-126). The API answers it with HTTP 401.
 *
 * @author mlanderoc
 * @version 1.0
 */
public class UnauthorizedException extends EcoPointException {

    private static final long serialVersionUID = 1L;
    public static final String ERROR_CODE = "ERR_UNAUTHORIZED";

    /**
     * Constructs an UnauthorizedException with a message in Spanish that does not
     * reveal whether the email exists.
     *
     * @param message message for the user
     */
    public UnauthorizedException(String message) {
        super(message, ERROR_CODE);
    }
}
