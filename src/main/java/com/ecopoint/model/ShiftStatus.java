package com.ecopoint.model;

/**
 * State of a sampling shift. Field data is only registered while the shift
 * is open (RN-021).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public enum ShiftStatus {

    ABIERTO("Abierto"),
    CERRADO("Cerrado");

    private final String displayName;

    ShiftStatus(String displayName) {
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
}
