package com.ecopoint.service;

import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.Container;
import com.ecopoint.model.EcologicalPoint;
import com.ecopoint.model.Institution;
import com.ecopoint.model.Zone;
import com.ecopoint.validation.EcologicalPointValidator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Business service responsible for managing {@link Institution}, {@link Zone},
 * and {@link EcologicalPoint} entities along with their regulatory containers.
 *
 * @author Estefania Marquez Martinez
 * @version 1.0
 */
public class EcologicalPointService {

    private final List<Institution> institutions;
    private final List<Zone> zones;
    private final List<EcologicalPoint> points;
    private int nextInstitutionId;
    private int nextZoneId;
    private int nextPointId;

    /**
     * Constructs an EcologicalPointService with empty in-memory collections.
     */
    public EcologicalPointService() {
        this.institutions = new ArrayList<>();
        this.zones = new ArrayList<>();
        this.points = new ArrayList<>();
        this.nextInstitutionId = 1;
        this.nextZoneId = 1;
        this.nextPointId = 1;
    }

    /**
     * Registers a new educational institution.
     *
     * @param institution institution to register
     * @return registered institution with assigned ID
     */
    public Institution registerInstitution(Institution institution) {
        EcologicalPointValidator.validateInstitutionData(institution);
        institution.setIdInstitution(nextInstitutionId++);
        institution.setName(institution.getName().trim());
        institution.setAddress(institution.getAddress().trim());
        institutions.add(institution);
        return institution;
    }

    /**
     * Registers a new zone within an existing institution.
     *
     * @param zone zone to register
     * @return registered zone with assigned ID
     */
    public Zone registerZone(Zone zone) {
        EcologicalPointValidator.validateZoneData(zone, zones);
        getInstitutionById(zone.getIdInstitution());

        zone.setIdZone(nextZoneId++);
        zone.setName(zone.getName().trim());
        zones.add(zone);
        return zone;
    }

    /**
     * Registers a new ecological point together with its three mandatory
     * Resolution 2184 of 2019 containers (BLANCO, VERDE, NEGRO).
     *
     * @param point ecological point to register
     * @return registered ecological point with assigned point and container IDs
     */
    public EcologicalPoint registerEcologicalPoint(EcologicalPoint point) {
        EcologicalPointValidator.validateEcologicalPointData(point);
        getZoneById(point.getIdZone());

        point.setIdPoint(nextPointId++);
        point.setLocation(point.getLocation().trim());
        point.setStatus(point.getStatus().trim().toUpperCase());
        point.setInitialStatus(point.getInitialStatus().trim().toUpperCase());

        int containerSequence = 1;
        for (Container container : point.getContainers()) {
            container.setIdPoint(point.getIdPoint());
            container.setIdContainer(containerSequence++);
            container.setColor(container.getColor().trim().toUpperCase());
            container.setWasteType(container.getWasteType().trim());
        }

        points.add(point);
        return point;
    }

    /**
     * Updates the relative map coordinates and operational status of an ecological point.
     *
     * @param idPoint   identifier of the ecological point
     * @param coordX    new horizontal relative percentage coordinate (0.00 - 100.00)
     * @param coordY    new vertical relative percentage coordinate (0.00 - 100.00)
     * @param newStatus new operational status
     * @return updated ecological point
     */
    public EcologicalPoint updatePointCoordinatesAndStatus(int idPoint, BigDecimal coordX,
                                                           BigDecimal coordY, String newStatus) {
        EcologicalPoint point = getEcologicalPointById(idPoint);
        EcologicalPointValidator.validateCoordinates(coordX, coordY);
        point.setCoordX(coordX);
        point.setCoordY(coordY);
        if (newStatus != null && !newStatus.trim().isEmpty()) {
            point.setStatus(newStatus.trim().toUpperCase());
        }
        return point;
    }

    /**
     * Finds an institution by ID.
     *
     * @param idInstitution institution identifier
     * @return matching institution
     */
    public Institution getInstitutionById(int idInstitution) {
        for (Institution inst : institutions) {
            if (inst.getIdInstitution() == idInstitution) {
                return inst;
            }
        }
        throw new ResourceNotFoundException("Institución", idInstitution);
    }

    /**
     * Finds a zone by ID.
     *
     * @param idZone zone identifier
     * @return matching zone
     */
    public Zone getZoneById(int idZone) {
        for (Zone zone : zones) {
            if (zone.getIdZone() == idZone) {
                return zone;
            }
        }
        throw new ResourceNotFoundException("Zona", idZone);
    }

    /**
     * Finds an ecological point by ID.
     *
     * @param idPoint ecological point identifier
     * @return matching ecological point
     */
    public EcologicalPoint getEcologicalPointById(int idPoint) {
        EcologicalPoint found = null;
        for (EcologicalPoint point : points) {
            if (point.getIdPoint() == idPoint) {
                found = point;
                break;
            }
        }
        EcologicalPointValidator.validatePointExists(found, idPoint);
        return found;
    }

    /**
     * Lists all ecological points, optionally filtered by zone ID.
     *
     * @param idZone optional zone identifier ({@code null} returns all points)
     * @return list of matching ecological points
     */
    public List<EcologicalPoint> listPointsByZone(Integer idZone) {
        if (idZone == null) {
            return Collections.unmodifiableList(points);
        }
        List<EcologicalPoint> filtered = new ArrayList<>();
        for (EcologicalPoint point : points) {
            if (point.getIdZone() == idZone) {
                filtered.add(point);
            }
        }
        return filtered;
    }
}
