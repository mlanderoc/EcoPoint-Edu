package com.ecopoint.model;

/**
 * Actions of the permission matrix (RN-081). Each constant is one row of the
 * matrix; {@link UserRole} decides which roles hold it.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public enum Permission {

    MANAGE_ADMINISTRATION("gestionar instituciones, fuentes y usuarios"),
    MANAGE_STUDIES("crear estudios y cambiar su estado"),
    MANAGE_SHIFTS("abrir, cerrar y reabrir turnos"),
    REGISTER_POPULATION("registrar la población del turno"),
    REGISTER_BAGS("registrar bolsas"),
    REGISTER_QUARTERING("registrar cuarteo y densidad"),
    REGISTER_HAZARDOUS_WASTE("registrar residuos peligrosos (grupo Sin color)"),
    VERIFY_RECORDS("verificar y corregir registros"),
    ANNUL_QUARTERING("anular un cuarteo no conforme"),
    ATTEND_ALERTS("atender alertas"),
    VIEW_INDICATORS("ver indicadores y reportes");

    private final String actionDescription;

    Permission(String actionDescription) {
        this.actionDescription = actionDescription;
    }

    /**
     * Returns the action in Spanish, ready to complete the sentence
     * "no tiene permiso para ...".
     *
     * @return action description in Spanish
     */
    public String getActionDescription() {
        return actionDescription;
    }
}
