package com.ecopoint.persistence;

import com.ecopoint.model.Crew;

import java.util.List;
import java.util.Optional;

/**
 * Storage contract for field crews (spec 002, R-050).
 *
 * @author mlanderoc
 * @version 1.0
 */
public interface CrewRepository {

    /**
     * Inserts a new crew (id null) or updates an existing one, together with
     * its list of members, in a single transaction.
     *
     * @param crew crew to save
     * @return the same crew, with its id assigned
     */
    Crew save(Crew crew);

    /**
     * Finds a crew with its responsible teacher and members.
     *
     * @param id crew id
     * @return the crew, or empty if it does not exist
     */
    Optional<Crew> findById(Long id);

    /**
     * Lists the crews whose responsible teacher belongs to an institution.
     *
     * @param institutionId institution id
     * @return crews with their members
     */
    List<Crew> findByInstitution(Long institutionId);
}
