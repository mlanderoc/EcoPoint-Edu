package com.ecopoint.model;

/**
 * Types of waste generation sources inside an educational institution (RN-003).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public enum SourceType {

    AULAS("Aulas"),
    OFICINAS("Oficinas"),
    TIENDA_ESCOLAR("Tienda escolar"),
    PAE("Programa de Alimentación Escolar (PAE)"),
    ZONAS_VERDES("Zonas verdes"),
    BANOS("Baños"),
    LABORATORIO("Laboratorio"),
    OTRA("Otra");

    private final String displayName;

    SourceType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the source type name shown to the user, in Spanish.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
