package com.ecopoint.service;

import com.ecopoint.model.Alert;
import com.ecopoint.model.AlertFactory;
import com.ecopoint.model.AlertType;
import com.ecopoint.model.CharacterizationSettings;
import com.ecopoint.model.QualityStatus;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.User;
import com.ecopoint.persistence.AlertRepository;
import com.ecopoint.persistence.QuarteringRepository;
import com.ecopoint.validation.QuarteringValidator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Keeps the quartering of a shift and its alert consistent (RN-093, RN-099).
 * If the sample no longer fits the weight of the shift, the quartering becomes
 * non-conforming; it has one open alert at most, closed once it is resolved.
 *
 * @author EstefaniaMarquez
 * @version 1.1
 */
@Component
public class QuarteringReconciler {

    private final QuarteringRepository quarterings;
    private final AlertRepository alerts;

    /**
     * Creates the component.
     *
     * @param quarterings quartering repository
     * @param alerts      alert repository
     */
    public QuarteringReconciler(QuarteringRepository quarterings, AlertRepository alerts) {
        this.quarterings = quarterings;
        this.alerts = alerts;
    }

    /**
     * Checks the quartering of a shift against its current net weight.
     *
     * @param shift    shift with its current bags
     * @param settings sample range and tolerance of the institution
     * @param actor    user whose change caused the check
     * @param now      moment of the change
     */
    public void reconcile(SamplingShift shift, CharacterizationSettings settings, User actor, LocalDateTime now) {
        Quartering quartering = shift.getQuartering();
        if (quartering == null || quartering.getQualityStatus() == QualityStatus.ANULADO) {
            return;
        }
        QualityStatus before = quartering.getQualityStatus();
        BigDecimal storedNet = quartering.getShiftNetWeight();
        BigDecimal currentNet = shift.calculateTotalNetWeight();
        quartering.updateShiftNetWeight(currentNet);
        if (QuarteringValidator.sampleFitsShift(quartering, settings)) {
            quartering.evaluateQuality(settings.qualityTolerancePercent());
            quarterings.save(quartering);
            syncAlert(quartering, shift.getCode(), actor, now, before);
            return;
        }
        if (currentNet.signum() <= 0 || quartering.getSampleWeight().compareTo(currentNet) > 0) {
            quartering.updateShiftNetWeight(storedNet);
        }
        quartering.markNonConforming();
        quarterings.save(quartering);
        keepOneAlert(AlertFactory.forSampleOutOfShift(quartering, shift.getCode(), currentNet, now), quartering, before);
    }

    private void keepOneAlert(Alert wanted, Quartering quartering, QualityStatus before) {
        List<Alert> open = openAlerts(quartering);
        if (open.isEmpty()) {
            if (before != QualityStatus.NO_CONFORME) {
                alerts.save(wanted);
            }
            return;
        }
        open.stream().filter(alert -> alert.reviseDescription(wanted.getDescription())).forEach(alerts::save);
    }

    private List<Alert> openAlerts(Quartering quartering) {
        return alerts.findOpenByTarget(AlertType.CUARTEO_NO_CONFORME, quartering.getRecordType(), quartering.getId());
    }

    /**
     * Leaves one open alert for a non-conforming quartering, or closes its open
     * alerts when it became conforming or was annulled (RN-099).
     *
     * @param quartering quartering already evaluated
     * @param shiftCode  code of its shift
     * @param actor      user whose action resolved it
     * @param now        moment of the change
     * @param before     quality status before the change
     */
    public void syncAlert(Quartering quartering, String shiftCode, User actor, LocalDateTime now, QualityStatus before) {
        if (quartering.getQualityStatus() == QualityStatus.NO_CONFORME) {
            keepOneAlert(AlertFactory.forNonConformingQuartering(quartering, shiftCode, now), quartering, before);
            return;
        }
        List<Alert> open = openAlerts(quartering);
        String note = quartering.getQualityStatus() == QualityStatus.ANULADO
                ? "Cerrada automáticamente: el cuarteo fue anulado. Motivo: " + quartering.getAnnulReason()
                : "Cerrada automáticamente: el cuarteo quedó conforme después de la corrección.";
        open.forEach(alert -> {
            alert.attend(actor, note, now);
            alerts.save(alert);
        });
    }
}
