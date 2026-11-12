package com.ecopoint.model;

/**
 * Sampling shifts of a day: morning and afternoon (RN-020).
 * The one-letter code is used in bag labels such as {@code AU-D3-M} (RN-031).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public enum ShiftType {

    MANANA("M", "Mañana"),
    TARDE("T", "Tarde");

    private final String code;
    private final String displayName;

    ShiftType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * Returns the one-letter code used in bag labels.
     *
     * @return "M" for morning, "T" for afternoon
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the shift name shown to the user, in Spanish.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
