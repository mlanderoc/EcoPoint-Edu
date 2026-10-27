package com.ecopoint.model;

/**
 * Represents a physical zone or sector within an educational institution
 * (e.g., Central Cafeteria, Classroom Block A, North Courtyard).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */

public class Zone {

    private int idZone;
    private int idInstitution;
    private String name;
    private String description;

    /**
     * Default constructor.
     */
    public Zone() {
    }

    /**
     * Constructs a Zone with all attributes.
     *
     * @param idZone        unique identifier of the zone
     * @param idInstitution identifier of the parent institution
     * @param name          name of the zone
     * @param description   description of the zone location or characteristics
     */
    public Zone(int idZone, int idInstitution, String name, String description) {
        this.idZone = idZone;
        this.idInstitution = idInstitution;
        this.name = name;
        this.description = description;
    }

    public int getIdZone() {
        return idZone;
    }

    public void setIdZone(int idZone) {
        this.idZone = idZone;
    }

    public int getIdInstitution() {
        return idInstitution;
    }

    public void setIdInstitution(int idInstitution) {
        this.idInstitution = idInstitution;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

