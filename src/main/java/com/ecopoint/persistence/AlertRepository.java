package com.ecopoint.persistence;

import com.ecopoint.model.Alert;

import java.util.List;
import java.util.Optional;

/**
 * Storage contract for data-quality alerts (spec 002, R-050). Attended alerts
 * are never deleted (RN-073).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public interface AlertRepository {

    /**
     * Inserts a new alert (id null) or stores the attention of an existing one.
     *
     * @param alert alert to save
     * @return the same alert, with its id assigned
     */
    Alert save(Alert alert);

    /**
     * Finds an alert by id.
     *
     * @param id alert id
     * @return the alert, or empty if it does not exist
     */
    Optional<Alert> findById(Long id);

    /**
     * Lists the alerts not yet attended, newest first.
     *
     * @return pending alerts
     */
    List<Alert> findPending();

    /**
     * Lists all alerts, newest first.
     *
     * @return alerts
     */
    List<Alert> findAll();

    /**
     * Lists the alerts of one study, newest first. An alert belongs to the study
     * of the shift of the record that caused it (spec 003, section 5.3).
     *
     * @param studyId study id
     * @return alerts of the study
     */
    List<Alert> findByStudy(Long studyId);
}
