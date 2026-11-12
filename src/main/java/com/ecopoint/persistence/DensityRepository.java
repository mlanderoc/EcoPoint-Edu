package com.ecopoint.persistence;

import com.ecopoint.model.DensityMeasurement;

import java.util.Optional;

/**
 * Storage contract for the density measurement of a shift (spec 002, R-050).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public interface DensityRepository {

    /**
     * Inserts a new measurement (id null) or updates an existing one.
     *
     * @param measurement measurement to save
     * @return the same measurement, with its id assigned
     */
    DensityMeasurement save(DensityMeasurement measurement);

    /**
     * Finds the density measurement of a shift.
     *
     * @param shiftId shift id
     * @return the measurement, or empty if the shift has none
     */
    Optional<DensityMeasurement> findByShift(Long shiftId);
}
