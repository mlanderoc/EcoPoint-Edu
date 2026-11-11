package com.ecopoint.model;

/**
 * Verification status of a field record checked against the paper form
 * (double registration, RN-060 to RN-063).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public enum VerificationStatus {

    PENDIENTE("Pendiente"),
    VERIFICADO("Verificado"),
    CORREGIDO("Corregido");

    private final String displayName;

    VerificationStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the status name shown to the user, in Spanish.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Indicates whether the record still has to be checked against the paper form.
     *
     * @return true when the record is pending
     */
    public boolean isPending() {
        return this == PENDIENTE;
    }
}
