package com.ecopoint.persistence;

import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.StudyStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Storage contract for characterization studies, with their 8 days, their
 * reopenings and their participating sources (spec 002, R-050).
 *
 * @author mlanderoc
 * @version 1.0
 */
public interface StudyRepository {

    /**
     * Inserts a new study with its 8 days (id null), or updates the status, the
     * dates and the new reopenings of an existing one, in a single transaction.
     *
     * @param study study to save
     * @return the same study, with its id assigned
     */
    CharacterizationStudy save(CharacterizationStudy study);

    /**
     * Finds a study with its days and reopenings.
     *
     * @param id study id
     * @return the study, or empty if it does not exist
     */
    Optional<CharacterizationStudy> findById(Long id);

    /**
     * Lists the studies of an institution, newest first.
     *
     * @param institutionId institution id
     * @return studies
     */
    List<CharacterizationStudy> findByInstitution(Long institutionId);

    /**
     * Stores a new status after the service has validated the transition.
     *
     * @param studyId study id
     * @param status  new status
     */
    void updateStatus(Long studyId, StudyStatus status);

    /**
     * Deletes a planned study with its days and participating sources (RN-019).
     * The database rejects it if the study already has shifts.
     *
     * @param studyId study id
     */
    void delete(Long studyId);

    /**
     * Replaces the set of sources taking part in the study.
     *
     * @param studyId   study id
     * @param sourceIds ids of the participating sources
     */
    void saveParticipatingSources(Long studyId, Set<Long> sourceIds);

    /**
     * Returns the sources taking part in the study.
     *
     * @param studyId study id
     * @return ids of the participating sources
     */
    Set<Long> findParticipatingSources(Long studyId);

    /**
     * Counts the records of the study still pending verification (RN-016).
     *
     * @param studyId study id
     * @return number of pending records
     */
    int countPendingRecords(Long studyId);

    /**
     * Counts the non-conforming quarterings of the study not yet re-weighed or annulled (RN-016).
     *
     * @param studyId study id
     * @return number of unresolved quarterings
     */
    int countUnresolvedQuarterings(Long studyId);
}
