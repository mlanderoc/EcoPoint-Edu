package com.ecopoint.exceptions;

/**
 * Exception thrown when syntactically valid data violates an institutional
 * or environmental business rule.
 * @author mlanderoc
 * @version 1.0
 */
public class BusinessRuleException extends EcoPointException {

    private static final long serialVersionUID = 1L;
    public static final String ERROR_CODE = "ERR_BUSINESS_RULE";

    /**
     * Constructs a BusinessRuleException with a message in Spanish.
     *
     * @param message business rule violation description in Spanish
     */
    public BusinessRuleException(String message) {
        super(message, ERROR_CODE);
    }

    /**
     * Constructs a BusinessRuleException with a custom error code and message in Spanish.
     *
     * @param message   business rule violation description in Spanish
     * @param errorCode custom business rule error code
     */
    public BusinessRuleException(String message, String errorCode) {
        super(message, errorCode);
    }
}