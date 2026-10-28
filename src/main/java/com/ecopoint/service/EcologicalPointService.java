package com.ecopoint.service;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.Container;
import com.ecopoint.model.EcologicalPoint;
import com.ecopoint.model.Institution;
import com.ecopoint.model.Zone;
import com.ecopoint.persistence.EcologicalPointRepository;
import com.ecopoint.persistence.FileEcologicalPointRepository;
import com.ecopoint.persistence.FileInstitutionRepository;
import com.ecopoint.persistence.FileZoneRepository;
import com.ecopoint.persistence.InstitutionRepository;
import com.ecopoint.persistence.ZoneRepository;
import com.ecopoint.validation.EcologicalPointValidator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Business service responsible for managing Institution, Zone, and EcologicalPoint
 * CRUD operations along with their regulatory containers.
 * Depends on repository interfaces applying Protected Variations and SOLID DIP.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class EcologicalPointService {

    private final InstitutionRepository institutionRepository;
    private final ZoneRepository zoneRepository;
    private final EcologicalPointRepository pointRepository;

    /**
     * Constructs an EcologicalPointService using default flat-file repositories.
     */
    public EcologicalPointService() {
        this(new FileInstitutionRepository(), new FileZoneRepository(), new FileEcologicalPointRepository());
    }

    /**
     * Constructs an EcologicalPointService with injected repository contracts.
     *
     * @param institutionRepository institution repository contract
     * @param zoneRepository        zone repository contract
     * @param pointRepository       ecological point repository contract
     */
    public EcologicalPointService(InstitutionRepository institutionRepository,
                                  ZoneRepository zoneRepository,
                                  EcologicalPointRepository pointRepository) {
        this.institutionRepository = institutionRepository;
        this.zoneRepository = zoneRepository;
        this.pointRepository = pointRepository;
    }

    /**
     * Registers a new educational institution.
     *
     * @param institution institution to register
     * @return registered institution with assigned ID
     */
    public Institution registerInstitution(Institution institution) {
        EcologicalPointValidator.validateInstitutionData(institution);
        institution.setIdInstitution(0);
        institution.setName(institution.getName().trim());
        institution.setAddress(institution.getAddress().trim());
        return institutionRepository.save(institution);
    }

    /**
     * Updates an existing educational institution.
     *
     * @param institution institution with updated attributes
     * @return updated institution
     */
    public Institution updateInstitution(Institution institution) {
        EcologicalPointValidator.validateInstitutionData(institution);
        getInstitutionById(institution.getIdInstitution());
        institution.setName(institution.getName().trim());
        institution.setAddress(institution.getAddress().trim());
        return institutionRepository.save(institution);
    }

    /**
     * Deletes an institution if it has no associated zones.
     *
     * @param idInstitution identifier of the institution to delete
     */
    public void deleteInstitution(int idInstitution) {
        getInstitutionById(idInstitution);
        for (Zone z : zoneRepository.findAll()) {
            if (z.getIdInstitution() == idInstitution) {
                throw new BusinessRuleException(
                        "No se puede eliminar la institución porque tiene zonas asociadas.");
            }
        }
        institutionRepository.deleteById(idInstitution);
    }

    /**
     * Lists all registered institutions.
     *
     * @return list of institutions
     */
    public List<Institution> listAllInstitutions() {
        return institutionRepository.findAll();
    }

    /**
     * Registers a new zone within an existing institution.
     *
     * @param zone zone to register
     * @return registered zone with assigned ID
     */
    public Zone registerZone(Zone zone) {
        zone.setIdZone(0);
        EcologicalPointValidator.validateZoneData(zone, zoneRepository.findAll());
        getInstitutionById(zone.getIdInstitution());

        zone.setName(zone.getName().trim());
        if (zone.getDescription() != null) {
            zone.setDescription(zone.getDescription().trim());
        }
        return zoneRepository.save(zone);
    }

    /**
     * Updates an existing zone.
     *
     * @param zone zone with updated attributes
     * @return updated zone
     */
    public Zone updateZone(Zone zone) {
        getZoneById(zone.getIdZone());
        EcologicalPointValidator.validateZoneData(zone, zoneRepository.findAll());
        getInstitutionById(zone.getIdInstitution());

        zone.setName(zone.getName().trim());
        if (zone.getDescription() != null) {
            zone.setDescription(zone.getDescription().trim());
        }
        return zoneRepository.save(zone);
    }

    /**
     * Deletes a zone if it has no associated ecological points.
     *
     * @param idZone identifier of the zone to delete
     */
    public void deleteZone(int idZone) {
        getZoneById(idZone);
        for (EcologicalPoint p : pointRepository.findAll()) {
            if (p.getIdZone() == idZone) {
                throw new BusinessRuleException(
                        "No se puede eliminar la zona porque tiene puntos ecológicos registrados.");
            }
        }
        zoneRepository.deleteById(idZone);
    }

    /**
     * Lists all registered zones.
     *
     * @return list of zones
     */
    public List<Zone> listAllZones() {
        return zoneRepository.findAll();
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

        point.setIdPoint(0);
        normalizePointAndContainers(point);
        return pointRepository.save(point);
    }

    /**
     * Updates an existing ecological point and its three containers.
     *
     * @param point ecological point with updated fields
     * @return updated ecological point
     */
    public EcologicalPoint updateEcologicalPoint(EcologicalPoint point) {
        getEcologicalPointById(point.getIdPoint());
        EcologicalPointValidator.validateEcologicalPointData(point);
        getZoneById(point.getIdZone());

        normalizePointAndContainers(point);
        return pointRepository.save(point);
    }

    /**
     * Deletes an ecological point by ID.
     *
     * @param idPoint identifier of the ecological point to delete
     */
    public void deleteEcologicalPoint(int idPoint) {
        getEcologicalPointById(idPoint);
        pointRepository.deleteById(idPoint);
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
        return pointRepository.save(point);
    }

    /**
     * Finds an institution by ID.
     *
     * @param idInstitution institution identifier
     * @return matching institution
     */
    public Institution getInstitutionById(int idInstitution) {
        Institution found = institutionRepository.findById(idInstitution);
        if (found == null) {
            throw new ResourceNotFoundException("Institución", idInstitution);
        }
        return found;
    }

    /**
     * Finds a zone by ID.
     *
     * @param idZone zone identifier
     * @return matching zone
     */
    public Zone getZoneById(int idZone) {
        Zone found = zoneRepository.findById(idZone);
        if (found == null) {
            throw new ResourceNotFoundException("Zona", idZone);
        }
        return found;
    }

    /**
     * Finds an ecological point by ID.
     *
     * @param idPoint ecological point identifier
     * @return matching ecological point
     */
    public EcologicalPoint getEcologicalPointById(int idPoint) {
        EcologicalPoint found = pointRepository.findById(idPoint);
        EcologicalPointValidator.validatePointExists(found, idPoint);
        return found;
    }

    /**
     * Lists all ecological points, optionally filtered by zone ID.
     *
     * @param idZone optional zone identifier (null returns all points)
     * @return list of matching ecological points
     */
    public List<EcologicalPoint> listPointsByZone(Integer idZone) {
        List<EcologicalPoint> all = pointRepository.findAll();
        if (idZone == null) {
            return all;
        }
        List<EcologicalPoint> filtered = new ArrayList<>();
        for (EcologicalPoint point : all) {
            if (point.getIdZone() == idZone) {
                filtered.add(point);
            }
        }
        return filtered;
    }

    private void normalizePointAndContainers(EcologicalPoint point) {
        point.setLocation(point.getLocation().trim());
        point.setStatus(point.getStatus().trim().toUpperCase());
        point.setInitialStatus(point.getInitialStatus().trim().toUpperCase());
        if (point.getObservations() != null) {
            point.setObservations(point.getObservations().trim());
        }
        int containerSequence = 1;
        for (Container container : point.getContainers()) {
            container.setIdPoint(point.getIdPoint());
            container.setIdContainer(containerSequence++);
            container.setColor(container.getColor().trim().toUpperCase());
            container.setWasteType(container.getWasteType().trim());
        }
    }
}
