package com.ecopoint.persistence;

import com.ecopoint.model.Quartering;

import java.util.Optional;

/**
 * Storage contract for the quartering of a shift (spec 002, R-050). A
 * quartering and its results are saved all or nothing (R-020, CA-105).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public interface QuarteringRepository {

    /**
     * Inserts a new quartering with its results (id null), or updates an
     * existing one replacing its results, in a single transaction.
     *
     * @param quartering quartering to save
     * @return the same quartering, with its id assigned
     */
    Quartering save(Quartering quartering);

    /**
     * Finds a quartering with its results by id.
     *
     * @param id quartering id
     * @return the quartering, or empty if it does not exist
     */
    Optional<Quartering> findById(Long id);

    /**
     * Finds the quartering of a shift with its results.
     *
     * @param shiftId shift id
     * @return the quartering, or empty if the shift has none
     */
    Optional<Quartering> findByShift(Long shiftId);
}
