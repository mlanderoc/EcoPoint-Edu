package com.ecopoint.model;

/**
 * Lifecycle of a characterization study (RN-014, RN-017).
 * A study only moves forward one step at a time:
 * PLANIFICADO, EN_EJECUCION, EN_DEPURACION, CERRADO.
 * The only way back is reopening a closed study for data cleaning
 * (CERRADO to EN_DEPURACION). A study being executed or cleaned can also be
 * cancelled (RN-018); CANCELADO is final and is not part of the normal order.
 *
 * @author EstefaniaMarquez
 * @version 2.0
 */
public enum StudyStatus {

    PLANIFICADO("Planificado"),
    EN_EJECUCION("En ejecución"),
    EN_DEPURACION("En depuración"),
    CERRADO("Cerrado"),
    CANCELADO("Cancelado");

    private final String displayName;

    StudyStatus(String displayName) {
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
     * Indicates whether the study may move from this status to the target status.
     *
     * @param target desired next status
     * @return true only for the next status in order, or for reopening a closed study
     */
    public boolean canTransitionTo(StudyStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case PLANIFICADO -> target == EN_EJECUCION;
            case EN_EJECUCION -> target == EN_DEPURACION;
            case EN_DEPURACION -> target == CERRADO;
            case CERRADO -> target == EN_DEPURACION;
            case CANCELADO -> false;
        };
    }

    /**
     * Indicates whether field data (population, bags, quartering, density)
     * may be registered in this status.
     *
     * @return true only while the study is being executed
     */
    public boolean allowsFieldRegistration() {
        return this == EN_EJECUCION;
    }

    /**
     * Indicates whether records may still be verified or corrected.
     *
     * @return true while executing or cleaning the data
     */
    public boolean allowsVerification() {
        return this == EN_EJECUCION || this == EN_DEPURACION;
    }

    /**
     * Indicates whether the study is read-only: its baseline is official or it
     * was cancelled.
     *
     * @return true when the study is closed or cancelled
     */
    public boolean isReadOnly() {
        return this == CERRADO || this == CANCELADO;
    }

    /**
     * Indicates whether the study may be cancelled (RN-018).
     *
     * @return true while executing or cleaning the data
     */
    public boolean allowsCancellation() {
        return this == EN_EJECUCION || this == EN_DEPURACION;
    }
}
