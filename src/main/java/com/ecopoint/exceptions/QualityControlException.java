package com.ecopoint.exceptions;

/**
 * Exception thrown when an operation is blocked because a quartering procedure
 * does not meet the 2 % quality control tolerance (non-conforming quartering).
 * It is a specialization of {@link BusinessRuleException} with its own error code
 * so the REST layer and the user interface can tell quality issues apart.
 *
 * @author mlanderoc
 * @version 1.0
 */
public class QualityControlException extends BusinessRuleException {

    private static final long serialVersionUID = 1L;
    public static final String ERROR_CODE = "ERR_QUALITY_CONTROL";

    /**
     * Constructs a QualityControlException with a message in Spanish.
     *
     * @param message quality control failure description in Spanish
     */
    public QualityControlException(String message) {
        super(message, ERROR_CODE);
    }
}
