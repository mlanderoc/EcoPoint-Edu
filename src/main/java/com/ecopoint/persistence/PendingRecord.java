package com.ecopoint.persistence;

/**
 * A field record still pending verification against the paper form, as listed
 * by the verification inbox (RN-060, spec 005).
 *
 * @param recordType kind of record, in Spanish (e.g. "Bolsa")
 * @param recordId   id of the record
 * @param shiftId    shift the record belongs to
 * @author EstefaniaMarquez
 * @version 1.0
 */
public record PendingRecord(String recordType, Long recordId, Long shiftId) {
}
