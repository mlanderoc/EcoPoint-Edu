package com.ecopoint.persistence;

import com.ecopoint.model.GenerationSource;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Storage contract for generation sources (spec 002, R-050).
 *
 * @author mlanderoc
 * @version 1.0
 */
public interface GenerationSourceRepository {

    /**
     * Inserts a new source (id null) or updates an existing one, including its polygon.
     *
     * @param source source to save
     * @return the same source, with its id assigned
     */
    GenerationSource save(GenerationSource source);

    /**
     * Finds a source by id.
     *
     * @param id source id
     * @return the source, or empty if it does not exist
     */
    Optional<GenerationSource> findById(Long id);

    /**
     * Lists the sources of an institution ordered by code.
     *
     * @param institutionId institution id
     * @return sources, active and inactive
     */
    List<GenerationSource> findByInstitution(Long institutionId);

    /**
     * Returns the codes already used in an institution, for the uniqueness rule (RN-002).
     *
     * @param institutionId institution id
     * @return set of codes
     */
    Set<String> findCodesByInstitution(Long institutionId);

    /**
     * Indicates whether the source has registered bags (RN-005).
     *
     * @param sourceId source id
     * @return true if at least one bag comes from the source
     */
    boolean hasBags(Long sourceId);

    /**
     * Deactivates a source: it keeps its history but is not offered for new records (RN-005).
     *
     * @param sourceId source id
     */
    void deactivate(Long sourceId);

    /**
     * Deletes a source without records. The database refuses it if the source has bags (CA-104).
     *
     * @param sourceId source id
     */
    void delete(Long sourceId);
}
