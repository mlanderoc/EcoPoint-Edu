package com.ecopoint.service;

import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.Alert;
import com.ecopoint.model.AlertType;
import com.ecopoint.model.Permission;
import com.ecopoint.model.User;
import com.ecopoint.persistence.AlertRepository;
import com.ecopoint.validation.AccessPolicy;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data-quality alerts: list them with filters and attend them (RN-070 to
 * RN-073; spec 003, section 5.3).
 *
 * @author craulgomez
 * @version 1.0
 */
@Service
public class AlertService {

    private final AlertRepository alerts;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param alerts alert repository
     * @param clock  clock that dates the attention
     */
    public AlertService(AlertRepository alerts, Clock clock) {
        this.alerts = alerts;
        this.clock = clock;
    }

    /**
     * Lists the alerts, newest first, with optional filters.
     *
     * @param studyId  study whose records caused the alert, or null for all
     * @param type     type of alert, or null for all
     * @param attended true or false to filter by attention, or null for all
     * @return alerts
     */
    public List<Alert> list(Long studyId, AlertType type, Boolean attended) {
        List<Alert> source = studyId == null ? alerts.findAll() : alerts.findByStudy(studyId);
        return source.stream()
                .filter(alert -> type == null || alert.getType() == type)
                .filter(alert -> attended == null || alert.isAttended() == attended)
                .toList();
    }

    /**
     * Marks an alert as attended with a note (RN-073). An attended alert is never deleted.
     *
     * @param actor user that attends it
     * @param id    alert id
     * @param note  what was done about it
     * @return the attended alert
     */
    public Alert attend(User actor, Long id, String note) {
        AccessPolicy.require(actor, Permission.ATTEND_ALERTS);
        Alert alert = alerts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Alerta", id));
        alert.attend(actor, note, LocalDateTime.now(clock));
        return alerts.save(alert);
    }
}
