package com.ecopoint.persistence;

import com.ecopoint.model.Correction;

import java.util.List;

/**
 * Storage contract for the correction history (spec 002, R-031). The history
 * only grows: there is no update or delete (CA-107).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public interface CorrectionRepository {

    /**
     * Appends a correction to the history.
     *
     * @param correction correction to store
     * @return id of the stored correction
     */
    Long save(Correction correction);

    /**
     * Lists the corrections of a record, oldest first.
     *
     * @param recordType kind of record, in Spanish (e.g. "Bolsa")
     * @param recordId   id of the record
     * @return corrections of the record
     */
    List<Correction> findByRecord(String recordType, Long recordId);
}
