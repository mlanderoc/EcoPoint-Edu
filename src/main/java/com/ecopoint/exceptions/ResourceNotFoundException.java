package com.ecopoint.exceptions;

/**
 * Exception thrown when a requested domain entity cannot be found by its identifier
 * or lookup key.
 *
 * @author mlanderoc
 * @version 1.0
 */
public class ResourceNotFoundException extends EcoPointException {

    private static final long serialVersionUID = 1L;
    public static final String ERROR_CODE = "ERR_NOT_FOUND";

    private final String resourceType;
    private final Object resourceId;

    /**
     * Constructs a ResourceNotFoundException for a specific resource type and identifier.
     *
     * @param resourceType human-readable resource name in Spanish (e.g., "Usuario", "Punto Ecológico")
     * @param resourceId   identifier or key that was not found
     */
    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super("El recurso " + resourceType + " con identificador '" + resourceId
                + "' no fue encontrado en el sistema.", ERROR_CODE);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * Returns the type of resource that was not found.
     *
     * @return resource type name
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Returns the identifier of the missing resource.
     *
     * @return resource identifier
     */
    public Object getResourceId() {
        return resourceId;
    }
}
