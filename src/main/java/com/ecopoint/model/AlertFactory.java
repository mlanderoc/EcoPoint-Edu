package com.ecopoint.model;

import com.ecopoint.exceptions.BusinessRuleException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Creates the automatic data-quality alerts (GRASP Creator, RN-070 to RN-072).
 * The domain decides when an alert is due and writes its message; saving it
 * is the job of the service layer.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public final class AlertFactory {

    /** Hours after closing a shift before pending records raise an alert (RN-072). */
    public static final long PENDING_VERIFICATION_HOURS = 24;

    private AlertFactory() {
    }

    /**
     * Creates the alert of a quartering outside the 2 % tolerance (RN-070).
     *
     * @param quartering non-conforming quartering
     * @param shiftCode  code of its shift, e.g. "D3-M"
     * @param at         date and time of the alert
     * @return the pending alert
     * @throws BusinessRuleException if the quartering is not non-conforming
     */
    public static Alert forNonConformingQuartering(Quartering quartering, String shiftCode, LocalDateTime at) {
        if (quartering.getQualityStatus() != QualityStatus.NO_CONFORME) {
            throw new BusinessRuleException("Solo un cuarteo no conforme genera esta alerta.");
        }
        String description = "Cuarteo no conforme en el turno " + shiftCode + ": la diferencia entre la "
                + "submuestra y lo clasificado es de " + spanish(quartering.calculateDifferencePercent())
                + " % (" + spanish(quartering.calculateDifferenceKg()) + " kg). Vuelva a pesar.";
        return new Alert(null, AlertType.CUARTEO_NO_CONFORME, quartering.getRecordType(), quartering.getId(),
                description, at);
    }

    /**
     * Creates the alert of a bag outside the plausibility limits (RN-071).
     *
     * @param bag atypical bag
     * @param at  date and time of the alert
     * @return the pending alert
     * @throws BusinessRuleException if the bag is not marked as atypical
     */
    public static Alert forAtypicalBag(WasteBag bag, LocalDateTime at) {
        if (!bag.isAtypical()) {
            throw new BusinessRuleException("Solo una bolsa marcada como atípica genera esta alerta.");
        }
        String density = bag.getDensity() == null ? "sin calcular" : spanish(bag.getDensity()) + " kg/m³";
        String description = "Valor atípico en la bolsa " + bag.getLabelText() + ": peso neto "
                + spanish(bag.getNetWeight()) + " kg, densidad " + density + ". Revise antes de verificar.";
        return new Alert(null, AlertType.VALOR_ATIPICO, bag.getRecordType(), bag.getId(), description, at);
    }

    /**
     * Indicates whether a closed shift should raise a pending-verification
     * alert: it still has pending records 24 hours after closing (RN-072).
     *
     * @param closedAt       date and time the shift was closed
     * @param now            current date and time
     * @param pendingRecords records still pending verification
     * @return true if the alert is due
     */
    public static boolean isPendingVerificationDue(LocalDateTime closedAt, LocalDateTime now, int pendingRecords) {
        return pendingRecords > 0 && closedAt != null && now != null
                && !now.isBefore(closedAt.plusHours(PENDING_VERIFICATION_HOURS));
    }

    /**
     * Creates the alert of a closed shift with records still pending (RN-072).
     *
     * @param shift          closed shift
     * @param pendingRecords records still pending verification
     * @param at             date and time of the alert
     * @return the pending alert
     */
    public static Alert forPendingVerification(SamplingShift shift, int pendingRecords, LocalDateTime at) {
        String description = "El turno " + shift.getCode() + " lleva más de " + PENDING_VERIFICATION_HOURS
                + " horas cerrado con " + pendingRecords
                + (pendingRecords == 1 ? " registro pendiente" : " registros pendientes")
                + " de verificación contra la planilla en papel.";
        return new Alert(null, AlertType.VERIFICACION_PENDIENTE, "Turno", shift.getId(), description, at);
    }

    private static String spanish(BigDecimal value) {
        return value == null ? "0" : value.toPlainString().replace('.', ',');
    }
}
