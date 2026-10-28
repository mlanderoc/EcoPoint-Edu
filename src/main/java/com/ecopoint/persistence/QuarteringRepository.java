package com.ecopoint.persistence;

import com.ecopoint.model.Quartering;
import java.util.List;

/**
 * Persistence contract for Quartering procedures and their results
 * applying Protected Variations and DIP.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public interface QuarteringRepository {

    /**
     * Saves a new quartering procedure or updates an existing one.
     *
     * @param quartering quartering procedure to persist
     * @return persisted quartering instance
     */
    Quartering save(Quartering quartering);

    /**
     * Finds a quartering procedure by unique identifier.
     *
     * @param idQuartering quartering identifier
     * @return matching quartering or null if not found
     */
    Quartering findById(int idQuartering);

    /**
     * Returns all persisted quartering procedures.
     *
     * @return list of quartering procedures
     */
    List<Quartering> findAll();

    /**
     * Deletes a quartering procedure by identifier.
     *
     * @param idQuartering identifier of the quartering to delete
     * @return true if deleted, false otherwise
     */
    boolean deleteById(int idQuartering);
}
