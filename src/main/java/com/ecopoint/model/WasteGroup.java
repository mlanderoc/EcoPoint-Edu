package com.ecopoint.model;

/**
 * Waste groups defined by Resolution 2184 of 2019 (Colombian national color code).
 * Every waste subcategory belongs to exactly one group (RN-040, RN-041).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public enum WasteGroup {

    BLANCO("Blanco", "Aprovechables", true, false),
    VERDE("Verde", "Orgánicos aprovechables", true, false),
    NEGRO("Negro", "No aprovechables", false, false),
    SIN_COLOR("Sin color", "Peligrosos y especiales", false, true);

    private final String displayName;
    private final String description;
    private final boolean recoverable;
    private final boolean hazardous;

    WasteGroup(String displayName, String description, boolean recoverable, boolean hazardous) {
        this.displayName = displayName;
        this.description = description;
        this.recoverable = recoverable;
        this.hazardous = hazardous;
    }

    /**
     * Returns the group name shown to the user, in Spanish.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the meaning of the group, in Spanish.
     *
     * @return group description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Indicates whether the group counts toward the recoverable fraction
     * (white and green groups).
     *
     * @return true for BLANCO and VERDE
     */
    public boolean isRecoverable() {
        return recoverable;
    }

    /**
     * Indicates whether the group holds hazardous or special waste, which only
     * a teacher or coordinator may register (RN-056).
     *
     * @return true for SIN_COLOR
     */
    public boolean isHazardous() {
        return hazardous;
    }
}
