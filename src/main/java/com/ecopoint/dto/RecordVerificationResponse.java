package com.ecopoint.dto;

import com.ecopoint.model.VerifiableRecord;

/**
 * A verifiable record with its double-entry state, as the verification routes
 * return it (RN-060 to RN-063).
 *
 * @param recordType   kind of record in the routes: population, bag, quartering or density
 * @param recordName   kind of record shown to the user, e.g. "Bolsa"
 * @param recordId     record id
 * @param shiftId      shift of the record
 * @param verification double-entry data, or null in the pending list
 * @author craulgomez
 * @version 1.0
 */
public record RecordVerificationResponse(String recordType, String recordName, Long recordId, Long shiftId,
                                         VerificationDto verification) {

    /**
     * Copies a verified or corrected record.
     *
     * @param routeName name of its kind in the routes
     * @param shiftId   shift of the record
     * @param record    domain record
     * @return the response
     */
    public static RecordVerificationResponse from(String routeName, Long shiftId, VerifiableRecord record) {
        return new RecordVerificationResponse(routeName, record.getRecordType(), record.getId(), shiftId,
                VerificationDto.from(record.getVerification()));
    }
}
