package com.ecopoint.persistence;

import com.ecopoint.model.Alert;
import java.util.List;

/**
 * Persistence contract for operational Alert entities applying Protected Variations and DIP.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public interface AlertRepository {

    /**
     * Saves a new alert or updates an existing alert in storage.
     *
     * @param alert alert instance to persist
     * @return persisted alert with assigned identifier
     */
    Alert save(Alert alert);

    /**
     * Finds an alert by unique identifier.
     *
     * @param idAlert alert identifier
     * @return matching alert or null if not found
     */
    Alert findById(int idAlert);

    /**
     * Returns all persisted alerts.
     *
     * @return list of alerts
     */
    List<Alert> findAll();

    /**
     * Deletes an alert by identifier.
     *
     * @param idAlert identifier of the alert to delete
     * @return true if deleted, false otherwise
     */
    boolean deleteById(int idAlert);
}
