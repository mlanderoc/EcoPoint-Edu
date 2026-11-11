package com.ecopoint.exceptions;

/**
 * Exception thrown when the role of the current user does not grant the
 * permission required to perform an operation (for example, a school student
 * trying to verify a record or to register hazardous waste).
 *
 * @author mlanderoc
 * @version 1.0
 */
public class AccessDeniedException extends EcoPointException {

    private static final long serialVersionUID = 1L;
    public static final String ERROR_CODE = "ERR_FORBIDDEN";

    /**
     * Constructs an AccessDeniedException with a message in Spanish.
     *
     * @param message access denial description in Spanish
     */
    public AccessDeniedException(String message) {
        super(message, ERROR_CODE);
    }

    /**
     * Constructs an AccessDeniedException naming the role and the denied action.
     *
     * @param roleName          display name of the user's role in Spanish
     * @param actionDescription description in Spanish of the denied action
     */
    public AccessDeniedException(String roleName, String actionDescription) {
        super("El rol " + roleName + " no tiene permiso para " + actionDescription + ".", ERROR_CODE);
    }
}
