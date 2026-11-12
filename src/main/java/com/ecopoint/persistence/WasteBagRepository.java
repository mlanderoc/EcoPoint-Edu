package com.ecopoint.persistence;

import com.ecopoint.model.WasteBag;

import java.util.List;
import java.util.Optional;

/**
 * Storage contract for waste bags (spec 002, R-050). Net weight and density
 * are calculated by the database (R-014).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public interface WasteBagRepository {

    /**
     * Inserts a new bag of a shift (id null) or updates the weights, atypical
     * mark and verification data of an existing one.
     *
     * @param shiftId shift the bag belongs to
     * @param bag     bag to save, already labeled by the shift
     * @return the same bag, with its id assigned
     */
    WasteBag save(Long shiftId, WasteBag bag);

    /**
     * Finds a bag by id.
     *
     * @param id bag id
     * @return the bag, or empty if it does not exist
     */
    Optional<WasteBag> findById(Long id);

    /**
     * Lists the bags of a shift in the order they were registered.
     *
     * @param shiftId shift id
     * @return bags of the shift
     */
    List<WasteBag> findByShift(Long shiftId);
}
