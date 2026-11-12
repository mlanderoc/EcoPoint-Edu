package com.ecopoint.persistence;

import com.ecopoint.model.VerifiableRecord;

import java.util.List;
import java.util.Optional;

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

    /**
     * Returns the shift of a verifiable record.
     *
     * @param recordType record type as {@code VerifiableRecord.getRecordType()} names it,
     *                   e.g. "Bolsa" or "Población del turno"
     * @param recordId   record id
     * @return the shift id, or empty if the record does not exist
     */
    Optional<Long> findShiftOfRecord(String recordType, Long recordId);
}
