package com.ecopoint.service;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.Alert;
import com.ecopoint.model.AlertType;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.Institution;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftPopulation;
import com.ecopoint.model.ShiftType;
import com.ecopoint.model.SourceType;
import com.ecopoint.model.StudyStatus;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.model.VerificationInfo;
import com.ecopoint.model.WasteBag;
import com.ecopoint.persistence.AlertRepository;
import com.ecopoint.persistence.GenerationSourceRepository;
import com.ecopoint.persistence.InstitutionRepository;
import com.ecopoint.persistence.ShiftRepository;
import com.ecopoint.persistence.StudyRepository;
import com.ecopoint.persistence.WasteBagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bags: registration, label, atypical alert and correction while pending
 * (RN-023, RN-030 to RN-037, RN-071, R-343, R-344, R-348).
 *
 * @author EstefaniaMarquez
 */
class WasteBagServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 8, 0);

    private WasteBagRepository bags;
    private ShiftRepository shifts;
    private AlertRepository alerts;
    private WasteBagService service;
    private SamplingShift shift;
    private final User student = new User(11L, "Laura", "Pérez", "l@unicesar.edu.co",
            UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
    private final GenerationSource classrooms = new GenerationSource(1L, 1L, "AU", "Aulas", SourceType.AULAS);

    @BeforeEach
    void setUp() {
        bags = mock(WasteBagRepository.class);
        shifts = mock(ShiftRepository.class);
        alerts = mock(AlertRepository.class);
        StudyRepository studies = mock(StudyRepository.class);
        GenerationSourceRepository sources = mock(GenerationSourceRepository.class);
        InstitutionRepository institutions = mock(InstitutionRepository.class);
        service = new WasteBagService(bags, shifts, studies, sources, institutions, alerts,
                Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

        List<LocalDate> dates = new ArrayList<>();
        IntStream.range(0, 8).forEach(i -> dates.add(LocalDate.of(2026, 11, 16).plusDays(i)));
        when(studies.findById(7L)).thenReturn(Optional.of(
                CharacterizationStudy.restore(7L, 1L, StudyStatus.EN_EJECUCION, dates, List.of())));
        when(studies.findParticipatingSources(7L)).thenReturn(Set.of(1L));
        when(institutions.findById(1L)).thenReturn(Optional.of(new Institution(1L, "I.E.", "V")));
        when(sources.findById(1L)).thenReturn(Optional.of(classrooms));

        shift = new SamplingShift(30L, 7L, 3, ShiftType.MANANA);
        when(shifts.findById(30L)).thenReturn(Optional.of(shift));
        when(bags.save(anyLong(), any())).thenAnswer(call -> {
            WasteBag bag = call.getArgument(1);
            if (bag.getId() == null) {
                bag.setId(50L);
            }
            return bag;
        });
    }

    @Test
    void ca206_bagWithoutPopulationIsRejectedWithTheMessageOfRn023() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> register("12.40", "0.40", "0.080"));

        assertEquals("Registre primero la población del turno.", error.getMessage());
    }

    @Test
    void ca032_bagGetsItsLabelNetWeightAndDensity() {
        withPopulation();

        WasteBag bag = register("12.40", "0.40", "0.080");

        assertEquals("AU-D3-M", bag.getLabelText());
        assertEquals(0, new BigDecimal("12.00").compareTo(bag.getNetWeight()));
        assertEquals(0, new BigDecimal("150.00").compareTo(bag.getDensity()));
        assertEquals(11L, bag.getVerification().getEnteredBy());
        assertFalse(bag.isAtypical());
        verify(alerts, never()).save(any());
    }

    @Test
    void ca205_tareGreaterThanGrossWeightIsInvalid() {
        withPopulation();

        InvalidDataException error = assertThrows(InvalidDataException.class, () -> register("3.20", "3.50", "0.080"));
        assertEquals("tara", error.getFieldName());
    }

    @Test
    void ca037_atypicalBagIsSavedWithItsAlert() {
        withPopulation();

        WasteBag bag = register("62.40", "0.40", "0.080");

        assertTrue(bag.isAtypical());
        ArgumentCaptor<Alert> alert = ArgumentCaptor.forClass(Alert.class);
        verify(alerts).save(alert.capture());
        assertEquals(AlertType.VALOR_ATIPICO, alert.getValue().getType());
        assertEquals(50L, alert.getValue().getTargetId());
    }

    @Test
    void r343_missingSourceIsInvalid() {
        withPopulation();

        assertThrows(InvalidDataException.class, () -> service.register(student, 30L, null,
                new BigDecimal("12.40"), new BigDecimal("0.40"), new BigDecimal("0.080")));
    }

    @Test
    void rn005_inactiveSourceDoesNotAcceptBags() {
        withPopulation();
        classrooms.deactivate();

        assertThrows(BusinessRuleException.class, () -> register("12.40", "0.40", "0.080"));
    }

    @Test
    void rn081_userWithoutPermissionCannotRegisterBags() {
        User inactive = new User(12L, "Ana", "Ruiz", "a@unicesar.edu.co", UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
        inactive.deactivate();

        assertThrows(AccessDeniedException.class, () -> service.register(inactive, 30L, 1L,
                new BigDecimal("12.40"), new BigDecimal("0.40"), new BigDecimal("0.080")));
    }

    @Test
    void r348_pendingBagIsCorrectedInItsOpenShift() {
        withPopulation();
        WasteBag bag = register("12.40", "0.40", "0.080");
        when(bags.findById(50L)).thenReturn(Optional.of(bag));
        when(bags.findShiftId(50L)).thenReturn(Optional.of(30L));

        WasteBag corrected = service.correctPending(student, 50L, new BigDecimal("13.40"), new BigDecimal("0.40"),
                new BigDecimal("0.080"));

        assertEquals(0, new BigDecimal("13.00").compareTo(corrected.getNetWeight()));
    }

    @Test
    void r348_verifiedBagIsCorrectedOnlyFromTheVerification() {
        withPopulation();
        WasteBag bag = register("12.40", "0.40", "0.080");
        bag.getVerification().markVerified(10L, NOW);
        when(bags.findById(50L)).thenReturn(Optional.of(bag));
        when(bags.findShiftId(50L)).thenReturn(Optional.of(30L));

        assertThrows(BusinessRuleException.class, () -> service.correctPending(student, 50L,
                new BigDecimal("13.40"), new BigDecimal("0.40"), new BigDecimal("0.080")));
    }

    private void withPopulation() {
        shift.registerPopulation(new ShiftPopulation(null, 300, 12, 6, new VerificationInfo(11L, NOW)));
    }

    private WasteBag register(String gross, String tare, String volume) {
        return service.register(student, 30L, 1L, new BigDecimal(gross), new BigDecimal(tare), new BigDecimal(volume));
    }
}
