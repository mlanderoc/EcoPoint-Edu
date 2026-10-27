package com.ecopoint.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents an ecological waste collection point installed inside a campus {@link Zone}.
 * Stores relative percentage coordinates ({@code coordX}, {@code coordY}) over the
 * institutional floor plan and aggregates the three mandatory {@link Container} instances.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class EcologicalPoint {

    private int idPoint;
    private int idZone;
    private String location;
    private String status;
    private String initialStatus;
    private BigDecimal coordX;
    private BigDecimal coordY;
    private String observations;
    private List<Container> containers;

    /**
     * Default constructor initializing the container list.
     */
    public EcologicalPoint() {
        this.containers = new ArrayList<>();
    }

    /**
     * Constructs an EcologicalPoint with all attributes.
     *
     * @param idPoint       unique identifier of the ecological point
     * @param idZone        identifier of the zone where the point is located
     * @param location      specific location description within the zone
     * @param status        current operational status (e.g., ACTIVO, CRITICO, MANTENIMIENTO)
     * @param initialStatus initial physical condition when registered
     * @param coordX        horizontal relative coordinate on the campus map (0.00 to 100.00%)
     * @param coordY        vertical relative coordinate on the campus map (0.00 to 100.00%)
     * @param observations  general observations about the point
     */
    public EcologicalPoint(int idPoint, int idZone, String location, String status,
                           String initialStatus, BigDecimal coordX, BigDecimal coordY,
                           String observations) {
        this.idPoint = idPoint;
        this.idZone = idZone;
        this.location = location;
        this.status = status;
        this.initialStatus = initialStatus;
        this.coordX = coordX;
        this.coordY = coordY;
        this.observations = observations;
        this.containers = new ArrayList<>();
    }

    /**
     * Adds a container to this ecological point (GRASP Creator / Information Expert).
     *
     * @param container container instance to associate with this point
     */
    public void addContainer(Container container) {
        if (container != null) {
            container.setIdPoint(this.idPoint);
            this.containers.add(container);
        }
    }

    /**
     * Verifies whether this ecological point contains exactly the three mandatory
     * containers (BLANCO, VERDE, NEGRO) defined by Resolution 2184 of 2019
     * (GRASP Information Expert).
     *
     * @return {@code true} if the point has exactly the 3 regulatory colors without duplicates
     */
    public boolean hasMandatoryThreeColors() {
        if (containers == null || containers.size() != 3) {
            return false;
        }
        Set<String> foundColors = new HashSet<>();
        for (Container container : containers) {
            if (container == null || !container.isRegulatoryColor()) {
                return false;
            }
            foundColors.add(container.getColor().trim().toUpperCase());
        }
        return foundColors.size() == 3;
    }

    public int getIdPoint() {
        return idPoint;
    }

    public void setIdPoint(int idPoint) {
        this.idPoint = idPoint;
    }

    public int getIdZone() {
        return idZone;
    }

    public void setIdZone(int idZone) {
        this.idZone = idZone;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInitialStatus() {
        return initialStatus;
    }

    public void setInitialStatus(String initialStatus) {
        this.initialStatus = initialStatus;
    }

    public BigDecimal getCoordX() {
        return coordX;
    }

    public void setCoordX(BigDecimal coordX) {
        this.coordX = coordX;
    }

    public BigDecimal getCoordY() {
        return coordY;
    }

    public void setCoordY(BigDecimal coordY) {
        this.coordY = coordY;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public List<Container> getContainers() {
        return containers;
    }

    public void setContainers(List<Container> containers) {
        this.containers = (containers != null) ? containers : new ArrayList<>();
    }
}
