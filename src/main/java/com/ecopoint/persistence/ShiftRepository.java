package com.ecopoint.persistence;

import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftStatus;

import java.util.List;
import java.util.Optional;

/**
 * Storage contract for sampling shifts (spec 002, R-050). Saving a shift stores
 * the shift itself, its population and its "no generation" marks; bags,
 * quartering and density have their own repositories.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public interface ShiftRepository {

    /**
     * Inserts a new shift (id null) or updates an existing one, with its
     * population and its "no generation" marks, in one transaction.
     *
     * @param shift shift to save
     * @return the same shift, with its id assigned
     */
    SamplingShift save(SamplingShift shift);

    /**
     * Finds a shift with everything recorded in it: population, bags,
     * quartering with its results, density and marks.
     *
     * @param id shift id
     * @return the shift, or empty if it does not exist
     */
    Optional<SamplingShift> findById(Long id);

    /**
     * Lists the shifts of a study, ordered by day and shift, each with
     * everything recorded in it.
     *
     * @param studyId study id
     * @return shifts of the study
     */
    List<SamplingShift> findByStudy(Long studyId);

    /**
     * Stores a new status; closing records the closing time and reopening clears it.
     *
     * @param shiftId shift id
     * @param status  new status
     */
    void updateStatus(Long shiftId, ShiftStatus status);
}
