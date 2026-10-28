package com.ecopoint.persistence;

import com.ecopoint.model.Institution;
import java.util.List;

/**
 * Persistence contract for Institution entities applying Protected Variations and DIP.
 * Allows transparent migration from flat files to a relational database.
 *
 * @author mlanderoc
 * @version 1.0
 */
public interface InstitutionRepository {

    /**
     * Saves a new institution or updates an existing one in storage.
     *
     * @param institution institution to persist
     * @return persisted institution with assigned identifier
     */
    Institution save(Institution institution);

    /**
     * Finds an institution by unique identifier.
     *
     * @param idInstitution institution identifier
     * @return matching institution or null if not found
     */
    Institution findById(int idInstitution);

    /**
     * Returns all persisted institutions.
     *
     * @return list of institutions
     */
    List<Institution> findAll();

    /**
     * Deletes an institution by identifier.
     *
     * @param idInstitution identifier of the institution to delete
     * @return true if deleted, false otherwise
     */
    boolean deleteById(int idInstitution);
}
