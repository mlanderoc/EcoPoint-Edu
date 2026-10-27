package com.ecopoint.model;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Represents an individual waste container (bin) belonging to an {@link EcologicalPoint}.
 * Complies with Colombian Resolution 2184 of 2019 color code:
 * {@code BLANCO} (recyclable), {@code VERDE} (organic), and {@code NEGRO} (non-recyclable).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class Container {

    public static final String COLOR_WHITE = "BLANCO";
    public static final String COLOR_GREEN = "VERDE";
    public static final String COLOR_BLACK = "NEGRO";

    private static final Set<String> MANDATORY_COLORS = Set.of(COLOR_WHITE, COLOR_GREEN, COLOR_BLACK);

    private int idPoint;
    private int idContainer;
    private String wasteType;
    private String color;
    private BigDecimal capacity;

    /**
     * Default constructor.
     */
    public Container() {
    }

    /**
     * Constructs a Container with all attributes.
     *
     * @param idPoint     identifier of the parent ecological point
     * @param idContainer sequential container identifier within the point
     * @param wasteType   type of waste accepted by the container
     * @param color       official container color (BLANCO, VERDE, NEGRO)
     * @param capacity    container capacity in liters or kilograms
     */
    public Container(int idPoint, int idContainer, String wasteType, String color, BigDecimal capacity) {
        this.idPoint = idPoint;
        this.idContainer = idContainer;
        this.wasteType = wasteType;
        this.color = color;
        this.capacity = capacity;
    }

    /**
     * Checks whether this container has a valid color under Resolution 2184 of 2019
     * (GRASP Information Expert).
     *
     * @return {@code true} if the color is BLANCO, VERDE, or NEGRO
     */
    public boolean isRegulatoryColor() {
        if (color == null) {
            return false;
        }
        return MANDATORY_COLORS.contains(color.trim().toUpperCase());
    }

    public int getIdPoint() {
        return idPoint;
    }

    public void setIdPoint(int idPoint) {
        this.idPoint = idPoint;
    }

    public int getIdContainer() {
        return idContainer;
    }

    public void setIdContainer(int idContainer) {
        this.idContainer = idContainer;
    }

    public String getWasteType() {
        return wasteType;
    }

    public void setWasteType(String wasteType) {
        this.wasteType = wasteType;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public BigDecimal getCapacity() {
        return capacity;
    }

    public void setCapacity(BigDecimal capacity) {
        this.capacity = capacity;
    }
}
