package com.ecopoint.exceptions;

/**
 * Abstract root exception for the EcoPoint Edu domain hierarchy.
 * @author mlanderoc
 * @version 1.0
 */
public abstract class EcoPointException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_ERROR_CODE = "ERR_ECOPOINT_GENERIC";

    private final String errorCode;

    /**
     * Constructs an EcoPointException with a user-facing message in Spanish
     * and the default error code.
     *
     * @param message descriptive error message in Spanish for the end user
     */
    protected EcoPointException(String message) {
        super(message);
        this.errorCode = DEFAULT_ERROR_CODE;
    }

    /**
     * Constructs an EcoPointException with a user-facing message in Spanish
     * and a specific semantic error code.
     *
     * @param message   descriptive error message in Spanish for the end user
     * @param errorCode semantic error code identifying the error category
     */
    protected EcoPointException(String message, String errorCode) {
        super(message);
        this.errorCode = (errorCode == null || errorCode.isBlank()) ? DEFAULT_ERROR_CODE : errorCode;
    }

    /**
     * Constructs an EcoPointException with a message, error code, and root cause.
     *
     * @param message   descriptive error message in Spanish for the end user
     * @param errorCode semantic error code identifying the error category
     * @param cause     underlying exception that caused this error
     */
    protected EcoPointException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = (errorCode == null || errorCode.isBlank()) ? DEFAULT_ERROR_CODE : errorCode;
    }

    /**
     * Returns the semantic error code associated with this exception.
     *
     * @return non-null semantic error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}
