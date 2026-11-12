package com.ecopoint.persistence;

import com.ecopoint.model.Evidence;
import com.ecopoint.model.EvidenceTarget;

import java.util.List;

/**
 * Storage contract for the metadata of evidence photos (spec 002, RN-090).
 * The image files themselves are stored outside the database.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public interface EvidenceRepository {

    /**
     * Stores the metadata of a new evidence file.
     *
     * @param evidence evidence to save
     * @return the same evidence, with its id assigned
     */
    Evidence save(Evidence evidence);

    /**
     * Lists the evidence attached to a record, oldest first.
     *
     * @param target   kind of record
     * @param targetId id of the record
     * @return evidence of the record
     */
    List<Evidence> findByTarget(EvidenceTarget target, Long targetId);
}
