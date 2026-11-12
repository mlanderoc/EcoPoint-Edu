package com.ecopoint.service;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.Alert;
import com.ecopoint.model.AlertType;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.persistence.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Alerts: filters and attention (RN-073).
 *
 * @author craulgomez
 */
class AlertServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 15, 0);

    private AlertRepository alerts;
    private AlertService service;
    private Alert atypical;
    private Alert quartering;
    private final User student = new User(11L, "Laura", "Pérez", "l@unicesar.edu.co",
            UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);

    @BeforeEach
    void setUp() {
        alerts = mock(AlertRepository.class);
        service = new AlertService(alerts, Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        atypical = new Alert(1L, AlertType.VALOR_ATIPICO, "Bolsa", 50L, "Valor atípico", NOW.minusHours(2));
        quartering = new Alert(2L, AlertType.CUARTEO_NO_CONFORME, "Cuarteo", 40L, "No conforme", NOW.minusHours(1));
        when(alerts.findAll()).thenReturn(List.of(quartering, atypical));
        when(alerts.findByStudy(7L)).thenReturn(List.of(atypical));
        when(alerts.findById(1L)).thenReturn(Optional.of(atypical));
        when(alerts.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void rn070_alertsAreFilteredByStudyTypeAndAttention() {
        assertEquals(List.of(atypical), service.list(7L, null, null));
        assertEquals(List.of(quartering), service.list(null, AlertType.CUARTEO_NO_CONFORME, null));
        assertEquals(2, service.list(null, null, false).size());
        assertTrue(service.list(null, null, true).isEmpty());
    }

    @Test
    void rn073_alertIsAttendedWithANoteWhoAndWhen() {
        Alert attended = service.attend(student, 1L, "Se repesó la bolsa y el valor era correcto.");

        assertTrue(attended.isAttended());
        assertEquals(11L, attended.getAttendedBy());
        assertEquals(NOW, attended.getAttendedAt());
    }

    @Test
    void rn073_attentionNeedsANoteAndHappensOnlyOnce() {
        assertThrows(InvalidDataException.class, () -> service.attend(student, 1L, " "));

        service.attend(student, 1L, "Revisado.");
        assertThrows(BusinessRuleException.class, () -> service.attend(student, 1L, "Otra vez."));
    }

    @Test
    void rn081_inactiveUserCannotAttendAlerts() {
        User inactive = new User(12L, "Ana", "Ruiz", "a@unicesar.edu.co", UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
        inactive.deactivate();

        assertThrows(AccessDeniedException.class, () -> service.attend(inactive, 1L, "Revisado."));
    }

    @Test
    void ca207_missingAlertIsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.attend(student, 99L, "Revisado."));
    }
}
