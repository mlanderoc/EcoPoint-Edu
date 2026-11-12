package com.ecopoint.model;

import java.util.Locale;

/**
 * Waste subcategory used to classify a quartering sample (paper, cardboard,
 * PET plastic, food waste, batteries...). Every subcategory belongs to exactly
 * one {@link WasteGroup} of Resolution 2184 of 2019 (RN-040 to RN-042).
 *
 * @author EstefaniaMarquez
 * @version 2.0
 */
public class WasteCategory {

    private Long id;
    private String name;
    private WasteGroup group;
    private String description;
    private boolean active;

    /**
     * Creates an empty, active subcategory.
     */
    public WasteCategory() {
        this.active = true;
    }

    /**
     * Creates an active subcategory.
     *
     * @param id          identifier, or null if not saved yet
     * @param name        subcategory name, e.g. "Cartón"
     * @param group       waste group it belongs to
     * @param description optional description
     */
    public WasteCategory(Long id, String name, WasteGroup group, String description) {
        this();
        this.id = id;
        this.name = name;
        this.group = group;
        this.description = description;
    }

    /**
     * Normalizes a subcategory name to compare it with others: trims spaces
     * and ignores upper and lower case, so "Cartón" and " cartón " are the
     * same name (RN-041).
     *
     * @param name name to normalize
     * @return normalized name, or an empty string if the name is null
     */
    public static String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Indicates whether the subcategory holds hazardous or special waste
     * (Sin color group, RN-056).
     *
     * @return true if its group is SIN_COLOR
     */
    public boolean isHazardous() {
        return group != null && group.isHazardous();
    }

    /**
     * Indicates whether the subcategory counts toward the recoverable fraction.
     *
     * @return true if its group is BLANCO or VERDE
     */
    public boolean isRecoverable() {
        return group != null && group.isRecoverable();
    }

    /**
     * Deactivates the subcategory: it keeps its history but is not offered in
     * new quarterings (RN-042).
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Activates the subcategory again.
     */
    public void activate() {
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WasteGroup getGroup() {
        return group;
    }

    public void setGroup(WasteGroup group) {
        this.group = group;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }
}
