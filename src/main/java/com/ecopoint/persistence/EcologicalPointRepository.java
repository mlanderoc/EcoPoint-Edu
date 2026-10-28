package com.ecopoint.persistence;

import com.ecopoint.model.EcologicalPoint;
import java.util.List;

/**
 * Persistence contract for EcologicalPoint entities and their three containers
 * applying Protected Variations and DIP.
 *
 * @author mlanderoc
 * @version 1.0
 */
public interface EcologicalPointRepository {

    /**
     * Saves a new ecological point or updates an existing point with its containers.
     *
     * @param point ecological point to persist
     * @return persisted ecological point with assigned identifiers
     */
    EcologicalPoint save(EcologicalPoint point);

    /**
     * Finds an ecological point by unique identifier.
     *
     * @param idPoint point identifier
     * @return matching ecological point or null if not found
     */
    EcologicalPoint findById(int idPoint);

    /**
     * Returns all persisted ecological points.
     *
     * @return list of ecological points
     */
    List<EcologicalPoint> findAll();

    /**
     * Deletes an ecological point by identifier.
     *
     * @param idPoint identifier of the ecological point to delete
     * @return true if deleted, false otherwise
     */
    boolean deleteById(int idPoint);
}
