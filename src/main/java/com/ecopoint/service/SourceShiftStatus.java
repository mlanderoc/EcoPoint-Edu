package com.ecopoint.service;

/**
 * State of a participating source in a shift (RN-024; spec 004, R-345).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public enum SourceShiftStatus {

    /** The source has at least one bag in the shift. */
    CON_GENERACION,
    /** The source was marked as having generated no waste. */
    SIN_GENERACION,
    /** Nothing was recorded yet; it blocks closing the shift. */
    PENDIENTE
}
