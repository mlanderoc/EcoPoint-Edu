package com.ecopoint.persistence;

import com.ecopoint.model.Institution;

import java.util.List;
import java.util.Optional;

/**
 * Storage contract for institutions (spec 002, R-050). Services depend on this
 * interface, never on its implementation (Protected Variations).
 *
 * @author mlanderoc
 * @version 1.0
 */
public interface InstitutionRepository {

    /**
     * Inserts a new institution (id null) or updates an existing one.
     *
     * @param institution institution to save
     * @return the same institution, with its id assigned
     */
    Institution save(Institution institution);

    /**
     * Finds an institution by id.
     *
     * @param id institution id
     * @return the institution, or empty if it does not exist
     */
    Optional<Institution> findById(Long id);

    /**
     * Lists all institutions ordered by name.
     *
     * @return institutions
     */
    List<Institution> findAll();

    /**
     * Indicates whether another institution already has this name.
     *
     * @param name institution name
     * @return true if the name is taken
     */
    boolean existsByName(String name);
}
