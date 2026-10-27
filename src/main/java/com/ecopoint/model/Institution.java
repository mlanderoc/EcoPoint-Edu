package com.ecopoint.model;

/**
 * Represents an educational institution (school, college, or university)
 * registered in the EcoPoint Edu system.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */

public class Institution {

    private int idInstitution;
    private String name;
    private String address;

    /**
     * Default constructor.
     */
    public Institution() {
    }

    /**
     * Constructs an Institution with all attributes.
     *
     * @param idInstitution unique identifier of the institution
     * @param name          institutional name
     * @param address       physical address of the institution
     */
    public Institution(int idInstitution, String name, String address) {
        this.idInstitution = idInstitution;
        this.name = name;
        this.address = address;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
