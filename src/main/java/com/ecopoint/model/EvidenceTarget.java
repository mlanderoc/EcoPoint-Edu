package com.ecopoint.model;

/**
 * Kind of record an evidence file (photo) can be attached to (RN-090).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public enum EvidenceTarget {

    SHIFT("Turno"),
    BAG("Bolsa"),
    QUARTERING("Cuarteo");

    private final String displayName;

    EvidenceTarget(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the record kind shown to the user, in Spanish.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
