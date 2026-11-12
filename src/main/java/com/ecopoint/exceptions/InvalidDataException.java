package com.ecopoint.exceptions;

/**
 * Exception thrown when user input fails syntactic, format, nullability,
 * or numeric range validations.
 * @author mlanderoc
 * @version 1.0
 */
public class InvalidDataException extends EcoPointException {

    private static final long serialVersionUID = 1L;
    public static final String ERROR_CODE = "ERR_INVALID_DATA";

    private final String fieldName;
    private final String problemDescription;

    /**
     * Constructs an InvalidDataException specifying the invalid field and reason.
     *
     * @param fieldName          name of the field that failed validation
     * @param problemDescription explanation in Spanish of why the value is invalid
     */
    public InvalidDataException(String fieldName, String problemDescription) {
        super("Dato inválido en el campo '" + fieldName + "': " + problemDescription, ERROR_CODE);
        this.fieldName = fieldName;
        this.problemDescription = problemDescription;
    }

    /**
     * Constructs an InvalidDataException with a direct message in Spanish.
     *
     * @param message descriptive validation error message in Spanish
     */
    public InvalidDataException(String message) {
        super(message, ERROR_CODE);
        this.fieldName = "general";
        this.problemDescription = message;
    }

    /**
     * Returns the name of the field that failed validation.
     *
     * @return field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns the reason why the value is invalid, without the field prefix of the message.
     * The API uses it to report the error next to the field (spec 003, CA-205).
     *
     * @return reason in Spanish
     */
    public String getProblemDescription() {
        return problemDescription;
    }
}