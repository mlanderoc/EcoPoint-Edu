package com.ecopoint.model;

/**
 * Represents a solid waste classification category associated with one of the
 * official container colors of Resolution 2184 of 2019.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class WasteCategory {

    private int idWaste;
    private String name;
    private String description;
    private String associatedColor;

    /**
     * Default constructor.
     */
    public WasteCategory() {
    }

    /**
     * Constructs a WasteCategory with all attributes.
     *
     * @param idWaste         unique identifier of the waste category
     * @param name            name of the waste category (e.g., Plástico PET, Orgánicos)
     * @param description     detailed description of the waste category
     * @param associatedColor official regulatory color (BLANCO, VERDE, NEGRO)
     */
    public WasteCategory(int idWaste, String name, String description, String associatedColor) {
        this.idWaste = idWaste;
        this.name = name;
        this.description = description;
        this.associatedColor = associatedColor;
    }

    public int getIdWaste() {
        return idWaste;
    }

    public void setIdWaste(int idWaste) {
        this.idWaste = idWaste;
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

    public String getAssociatedColor() {
        return associatedColor;
    }

    public void setAssociatedColor(String associatedColor) {
        this.associatedColor = associatedColor;
    }
}
