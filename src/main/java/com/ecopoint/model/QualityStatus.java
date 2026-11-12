package com.ecopoint.model;

/**
 * Result of the 2 % quality control of a quartering procedure (RN-052).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public enum QualityStatus {

    CONFORME("Conforme"),
    NO_CONFORME("No conforme"),
    ANULADO("Anulado");

    private final String displayName;

    QualityStatus(String displayName) {
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
     * Indicates whether this status prevents closing the study until it is
     * resolved by re-weighing or annulment (RN-016).
     *
     * @return true for a non-conforming quartering
     */
    public boolean blocksStudyClosure() {
        return this == NO_CONFORME;
    }

    /**
     * Indicates whether the quartering takes part in the baseline indicators.
     * Annulled quarterings are excluded.
     *
     * @return false only for an annulled quartering
     */
    public boolean countsForIndicators() {
        return this != ANULADO;
    }
}
