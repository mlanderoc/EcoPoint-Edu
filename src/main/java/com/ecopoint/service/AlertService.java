package com.ecopoint.service;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.Alert;
import com.ecopoint.model.ContainerDetail;
import com.ecopoint.model.InspectionRecord;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Business service responsible for evaluating field inspections and automatically
 * generating operational {@link Alert} instances when containers overflow or
 * exhibit improper waste separation (GRASP Creator &amp; SOLID SRP).
 *
 * @author craulgomez
 * @version 1.0
 */
public class AlertService {

    private final List<Alert> alerts;
    private int nextAlertId;

    /**
     * Constructs an AlertService with an empty in-memory alert registry.
     */
    public AlertService() {
        this.alerts = new ArrayList<>();
        this.nextAlertId = 1;
    }

    /**
     * Evaluates all container details of an {@link InspectionRecord} and automatically
     * creates alerts for overflow ({@code DESBORDADO}) or improper separation.
     *
     * @param record completed field inspection record
     * @return list of alerts generated during this evaluation
     */
    public List<Alert> evaluateAndGenerateAutomaticAlerts(InspectionRecord record) {
        List<Alert> generated = new ArrayList<>();
        if (record == null || record.getContainerDetails() == null) {
            return generated;
        }

        for (ContainerDetail detail : record.getContainerDetails()) {
            if (detail == null) {
                continue;
            }
            if (detail.requiresOverflowAlert()) {
                Alert overflowAlert = new Alert(
                        nextAlertId++,
                        detail.getIdContainerDetail(),
                        Alert.TYPE_OVERFLOW,
                        false,
                        "Alerta de desbordamiento detectada en el punto ecológico #" + record.getIdPoint()
                                + " (recipiente #" + detail.getIdContainer() + ").",
                        LocalDateTime.now()
                );
                alerts.add(overflowAlert);
                generated.add(overflowAlert);
            }
            if (detail.requiresImproperSeparationAlert()) {
                Alert separationAlert = new Alert(
                        nextAlertId++,
                        detail.getIdContainerDetail(),
                        Alert.TYPE_IMPROPER_SEPARATION,
                        false,
                        "Alerta por mala separación de residuos en el punto ecológico #" + record.getIdPoint()
                                + " (recipiente #" + detail.getIdContainer() + ").",
                        LocalDateTime.now()
                );
                alerts.add(separationAlert);
                generated.add(separationAlert);
            }
        }
        return generated;
    }

    /**
     * Marks an active alert as attended.
     *
     * @param idAlert identifier of the alert to attend
     * @return updated alert instance
     * @throws ResourceNotFoundException if the alert does not exist
     * @throws BusinessRuleException     if the alert was already attended
     */
    public Alert markAlertAsAttended(int idAlert) {
        Alert alert = getAlertById(idAlert);
        if (alert.isAttended()) {
            throw new BusinessRuleException(
                    "La alerta con identificador '" + idAlert + "' ya fue marcada como atendida anteriormente.");
        }
        alert.markAsAttended();
        return alert;
    }

    /**
     * Finds an alert by its identifier.
     *
     * @param idAlert alert identifier
     * @return matching alert
     * @throws ResourceNotFoundException if no alert matches the ID
     */
    public Alert getAlertById(int idAlert) {
        for (Alert alert : alerts) {
            if (alert.getIdAlert() == idAlert) {
                return alert;
            }
        }
        throw new ResourceNotFoundException("Alerta", idAlert);
    }

    /**
     * Lists all pending (unattended) alerts.
     *
     * @return list of unattended alerts
     */
    public List<Alert> listPendingAlerts() {
        List<Alert> pending = new ArrayList<>();
        for (Alert alert : alerts) {
            if (!alert.isAttended()) {
                pending.add(alert);
            }
        }
        return pending;
    }
}