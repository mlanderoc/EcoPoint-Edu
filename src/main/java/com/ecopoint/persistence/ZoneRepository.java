package com.ecopoint.persistence;

import com.ecopoint.model.Zone;
import java.util.List;

/**
 * Persistence contract for Zone entities applying Protected Variations and DIP.
 * Allows transparent migration from flat files to a relational database.
 *
 * @author mlanderoc
 * @version 1.0
 */
public interface ZoneRepository {

    /**
     * Saves a new zone or updates an existing zone in storage.
     *
     * @param zone zone to persist
     * @return persisted zone with assigned identifier
     */
    Zone save(Zone zone);

    /**
     * Finds a zone by unique identifier.
     *
     * @param idZone zone identifier
     * @return matching zone or null if not found
     */
    Zone findById(int idZone);

    /**
     * Returns all persisted zones.
     *
     * @return list of zones
     */
    List<Zone> findAll();

    /**
     * Deletes a zone by identifier.
     *
     * @param idZone identifier of the zone to delete
     * @return true if deleted, false otherwise
     */
    boolean deleteById(int idZone);
}
