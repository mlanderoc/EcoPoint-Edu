package com.ecopoint.exceptions;

/**
 * Exception thrown when an underlying data storage or persistence operation fails.
 * @author mlanderoc
 * @version 1.0
 */
public class PersistenceException extends EcoPointException {

    private static final long serialVersionUID = 1L;
    public static final String ERROR_CODE = "ERR_PERSISTENCE";

    /**
     * Constructs a PersistenceException wrapping the original cause.
     *
     * @param operationContext description of the persistence operation that failed
     * @param cause            underlying technical exception
     */
    public PersistenceException(String operationContext, Throwable cause) {
        super("Error de persistencia durante la operación [" + operationContext + "].", ERROR_CODE, cause);
    }

    /**
     * Constructs a PersistenceException with a message in Spanish.
     *
     * @param message descriptive persistence error message in Spanish
     */
    public PersistenceException(String message) {
        super(message, ERROR_CODE);
    }
}
