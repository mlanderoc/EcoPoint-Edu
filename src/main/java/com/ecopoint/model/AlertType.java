package com.ecopoint.model;

/**
 * Types of automatic data-quality alerts (RN-070 to RN-072).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public enum AlertType {

    CUARTEO_NO_CONFORME("Cuarteo no conforme"),
    VALOR_ATIPICO("Valor atípico"),
    VERIFICACION_PENDIENTE("Verificación pendiente");

    private final String displayName;

    AlertType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the alert type name shown to the user, in Spanish.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
