package com.ecopoint.service;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.Alert;
import com.ecopoint.model.ContainerDetail;
import com.ecopoint.model.InspectionRecord;
import com.ecopoint.persistence.AlertRepository;
import com.ecopoint.persistence.FileAlertRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Business service responsible for evaluating field inspections and automatically
 * generating operational Alert instances when containers overflow or exhibit
 * improper waste separation. Depends on AlertRepository applying Protected Variations.
 *
 * @author craulgomez
 * @version 1.0
 */
public class AlertService {

    private final AlertRepository alertRepository;

    /**
     * Constructs an AlertService using the default flat-file repository.
     */
    public AlertService() {
        this(new FileAlertRepository());
    }

    /**
     * Constructs an AlertService with an injected AlertRepository contract.
     *
     * @param alertRepository alert repository implementation
     */
    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /**
     * Evaluates all container details of an InspectionRecord and automatically
     * creates and persists alerts for overflow or improper separation.
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
                        0,
                        detail.getIdContainerDetail(),
                        Alert.TYPE_OVERFLOW,
                        false,
                        "Alerta de desbordamiento detectada en el punto ecológico #" + record.getIdPoint()
                                + " (recipiente #" + detail.getIdContainer() + ").",
                        LocalDateTime.now()
                );
                generated.add(alertRepository.save(overflowAlert));
            }
            if (detail.requiresImproperSeparationAlert()) {
                Alert separationAlert = new Alert(
                        0,
                        detail.getIdContainerDetail(),
                        Alert.TYPE_IMPROPER_SEPARATION,
                        false,
                        "Alerta por mala separación de residuos en el punto ecológico #" + record.getIdPoint()
                                + " (recipiente #" + detail.getIdContainer() + ").",
                        LocalDateTime.now()
                );
                generated.add(alertRepository.save(separationAlert));
            }
        }
        return generated;
    }

    /**
     * Marks an active alert as attended and persists the change.
     *
     * @param idAlert identifier of the alert to attend
     * @return updated alert instance
     */
    public Alert markAlertAsAttended(int idAlert) {
        Alert alert = getAlertById(idAlert);
        if (alert.isAttended()) {
            throw new BusinessRuleException(
                    "La alerta con identificador '" + idAlert + "' ya fue marcada como atendida anteriormente.");
        }
        alert.markAsAttended();
        return alertRepository.save(alert);
    }

    /**
     * Deletes an alert by its identifier.
     *
     * @param idAlert identifier of the alert to delete
     */
    public void deleteAlert(int idAlert) {
        getAlertById(idAlert);
        alertRepository.deleteById(idAlert);
    }

    /**
     * Finds an alert by its identifier.
     *
     * @param idAlert alert identifier
     * @return matching alert
     */
    public Alert getAlertById(int idAlert) {
        Alert found = alertRepository.findById(idAlert);
        if (found == null) {
            throw new ResourceNotFoundException("Alerta", idAlert);
        }
        return found;
    }

    /**
     * Lists all pending (unattended) alerts.
     *
     * @return list of unattended alerts
     */
    public List<Alert> listPendingAlerts() {
        List<Alert> pending = new ArrayList<>();
        for (Alert alert : alertRepository.findAll()) {
            if (!alert.isAttended()) {
                pending.add(alert);
            }
        }
        return pending;
    }

    /**
     * Lists all alerts regardless of attended state.
     *
     * @return list of all alerts
     */
    public List<Alert> listAllAlerts() {
        return alertRepository.findAll();
    }
}
