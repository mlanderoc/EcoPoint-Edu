package com.ecopoint.persistence;

import com.ecopoint.model.VerifiableRecord;

import java.util.List;

/**
 * Storage contract for the double registration (spec 002, R-030): stores the
 * verification status of any verifiable record and lists what is still pending.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public interface VerificationRepository {

    /**
     * Stores the verification status, verifier and date of a record
     * (population, bag, quartering or density measurement).
     *
     * @param record verifiable record with its verification data already updated
     */
    void updateVerification(VerifiableRecord record);

    /**
     * Lists the records of a study still pending verification.
     *
     * @param studyId study id
     * @return pending records ordered by shift
     */
    List<PendingRecord> findPendingByStudy(Long studyId);
}
