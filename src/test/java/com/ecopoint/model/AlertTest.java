package com.ecopoint.model;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Alert} and {@link AlertFactory} (RN-070 to RN-073,
 * alert side of CA-033 and CA-053).
 *
 * @author EstefaniaMarquez
 * @version 2.0
 */
class AlertTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 3, 4, 13, 0);
    private static final User STUDENT =
            new User(2L, "Luis", "Gómez", "luis@unicesar.edu.co", UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);

    private static Quartering nonConformingQuartering() {
        Quartering quartering = new Quartering(5L, 1L, new BigDecimal("120.00"), new BigDecimal("50.00"),
                new VerificationInfo(10L, AT));
        quartering.addResult(new QuarteringResult(1L, "Papel", WasteGroup.BLANCO, new BigDecimal("20.00")));
        quartering.addResult(new QuarteringResult(2L, "Comida", WasteGroup.VERDE, new BigDecimal("28.50")));
        quartering.evaluateQuality(new BigDecimal("2"));
        return quartering;
    }

    private static Alert pendingAlert() {
        return AlertFactory.forNonConformingQuartering(nonConformingQuartering(), "D3-M", AT);
    }

    @Test
    void ca053_nonConformingQuarteringRaisesAnAlert() {
        Alert alert = pendingAlert();

        assertEquals(AlertType.CUARTEO_NO_CONFORME, alert.getType());
        assertEquals("Cuarteo", alert.getTargetType());
        assertEquals(5L, alert.getTargetId());
        assertEquals("Cuarteo no conforme en el turno D3-M: la diferencia entre la submuestra y lo clasificado "
                + "es de 3,00 % (1,50 kg). Vuelva a pesar.", alert.getDescription());
        assertFalse(alert.isAttended());
    }

    @Test
    void rn070_conformingQuarteringDoesNotRaiseAnAlert() {
        Quartering conforming = new Quartering(5L, 1L, new BigDecimal("120"), new BigDecimal("50.00"),
                new VerificationInfo(10L, AT));
        conforming.addResult(new QuarteringResult(1L, "Papel", WasteGroup.BLANCO, new BigDecimal("49.50")));
        conforming.evaluateQuality(new BigDecimal("2"));

        assertThrows(BusinessRuleException.class,
                () -> AlertFactory.forNonConformingQuartering(conforming, "D3-M", AT));
    }

    @Test
    void ca033_atypicalBagRaisesAnAlert() {
        SamplingShift shift = new SamplingShift(1L, 1L, 3, ShiftType.MANANA);
        WasteBag heavy = new WasteBag(8L, 1L, "AU", new BigDecimal("70.50"), new BigDecimal("0.50"),
                new BigDecimal("0.300"), new VerificationInfo(10L, AT));
        shift.addBag(heavy);
        heavy.setAtypical(true);

        Alert alert = AlertFactory.forAtypicalBag(heavy, AT);

        assertEquals(AlertType.VALOR_ATIPICO, alert.getType());
        assertEquals("Valor atípico en la bolsa AU-D3-M: peso neto 70,00 kg, densidad 233,33 kg/m³. "
                + "Revise antes de verificar.", alert.getDescription());
    }

    @Test
    void rn071_normalBagDoesNotRaiseAnAlert() {
        WasteBag normal = new WasteBag(8L, 1L, "AU", new BigDecimal("12.40"), new BigDecimal("0.40"),
                new BigDecimal("0.080"), new VerificationInfo(10L, AT));

        assertThrows(BusinessRuleException.class, () -> AlertFactory.forAtypicalBag(normal, AT));
    }

    @Test
    void rn072_pendingVerificationAlertIsDueAfterTwentyFourHours() {
        LocalDateTime closed = LocalDateTime.of(2026, 3, 4, 18, 0);

        assertFalse(AlertFactory.isPendingVerificationDue(closed, closed.plusHours(23), 3));
        assertTrue(AlertFactory.isPendingVerificationDue(closed, closed.plusHours(24), 3));
        assertFalse(AlertFactory.isPendingVerificationDue(closed, closed.plusHours(30), 0));

        Alert alert = AlertFactory.forPendingVerification(new SamplingShift(9L, 1L, 3, ShiftType.TARDE), 3, AT);
        assertEquals(AlertType.VERIFICACION_PENDIENTE, alert.getType());
        assertTrue(alert.getDescription().contains("D3-T"));
    }

    @Test
    void rn073_attendingNeedsANoteAndKeepsWhoAndWhen() {
        Alert alert = pendingAlert();
        assertThrows(InvalidDataException.class, () -> alert.attend(STUDENT, " ", AT));

        alert.attend(STUDENT, "Se repesó la submuestra", AT.plusHours(1));

        assertTrue(alert.isAttended());
        assertEquals(STUDENT.getId(), alert.getAttendedBy());
        assertEquals(AT.plusHours(1), alert.getAttendedAt());
        assertEquals("Se repesó la submuestra", alert.getAttendNote());
    }

    @Test
    void rn073_alertIsNotAttendedTwice() {
        Alert alert = pendingAlert();
        alert.attend(STUDENT, "Se repesó", AT);

        assertThrows(BusinessRuleException.class, () -> alert.attend(STUDENT, "Otra vez", AT));
    }

    @Test
    void rn081_inactiveUserCannotAttendAlerts() {
        User inactive = new User(3L, "Eva", "Ríos", "eva@unicesar.edu.co", UserRole.DOCENTE, 1L);
        inactive.deactivate();

        assertThrows(AccessDeniedException.class, () -> pendingAlert().attend(inactive, "nota", AT));
    }
}
