package com.ecopoint.persistence;

import com.ecopoint.model.InspectionDay;
import com.ecopoint.model.InspectionRecord;
import java.util.List;

/**
 * Persistence contract for InspectionDay and InspectionRecord entities
 * applying Protected Variations and DIP.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public interface InspectionRepository {

    /**
     * Saves a new inspection day or updates an existing one.
     *
     * @param day inspection day to persist
     * @return persisted inspection day
     */
    InspectionDay saveDay(InspectionDay day);

    /**
     * Finds an inspection day by unique identifier.
     *
     * @param idDay inspection day identifier
     * @return matching inspection day or null if not found
     */
    InspectionDay findDayById(int idDay);

    /**
     * Returns all persisted inspection days.
     *
     * @return list of inspection days
     */
    List<InspectionDay> findAllDays();

    /**
     * Deletes an inspection day by identifier.
     *
     * @param idDay identifier of the inspection day to delete
     * @return true if deleted, false otherwise
     */
    boolean deleteDayById(int idDay);

    /**
     * Saves a new field inspection record or updates an existing one.
     *
     * @param record inspection record to persist
     * @return persisted inspection record
     */
    InspectionRecord saveRecord(InspectionRecord record);

    /**
     * Finds an inspection record by unique identifier.
     *
     * @param idRecord inspection record identifier
     * @return matching inspection record or null if not found
     */
    InspectionRecord findRecordById(int idRecord);

    /**
     * Returns all persisted inspection records.
     *
     * @return list of inspection records
     */
    List<InspectionRecord> findAllRecords();

    /**
     * Deletes an inspection record by identifier.
     *
     * @param idRecord identifier of the inspection record to delete
     * @return true if deleted, false otherwise
     */
    boolean deleteRecordById(int idRecord);
}
