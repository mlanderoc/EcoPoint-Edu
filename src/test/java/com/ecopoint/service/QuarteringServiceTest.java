package com.ecopoint.service;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.Alert;
import com.ecopoint.model.AlertType;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.DensityMeasurement;
import com.ecopoint.model.Institution;
import com.ecopoint.model.QualityStatus;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftPopulation;
import com.ecopoint.model.ShiftType;
import com.ecopoint.model.StudyStatus;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.model.VerificationInfo;
import com.ecopoint.model.WasteBag;
import com.ecopoint.model.WasteCategory;
import com.ecopoint.model.WasteGroup;
import com.ecopoint.persistence.AlertRepository;
import com.ecopoint.persistence.DensityRepository;
import com.ecopoint.persistence.InstitutionRepository;
import com.ecopoint.persistence.QuarteringRepository;
import com.ecopoint.persistence.ShiftRepository;
import com.ecopoint.persistence.StudyRepository;
import com.ecopoint.persistence.WasteCategoryRepository;
import com.ecopoint.service.QuarteringService.ResultInput;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Quartering and density of a shift (RN-050 to RN-057, RN-070, CA-050 to CA-057).
 *
 * @author craulgomez
 */
class QuarteringServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 11, 0);
    private static final Long PAPER = 1L;
    private static final Long FOOD = 2L;
    private static final Long BATTERIES = 3L;

    private QuarteringRepository quarterings;
    private AlertRepository alerts;
    private QuarteringService service;
    private DensityService densityService;
    private SamplingShift shift;
    private final User student = new User(11L, "Laura", "Pérez", "l@unicesar.edu.co",
            UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
    private final User teacher = new User(10L, "Daniela", "Arias", "d@unicesar.edu.co", UserRole.DOCENTE, 1L);

    @BeforeEach
    void setUp() {
        quarterings = mock(QuarteringRepository.class);
        alerts = mock(AlertRepository.class);
        ShiftRepository shifts = mock(ShiftRepository.class);
        StudyRepository studies = mock(StudyRepository.class);
        InstitutionRepository institutions = mock(InstitutionRepository.class);
        WasteCategoryRepository categories = mock(WasteCategoryRepository.class);
        DensityRepository densities = mock(DensityRepository.class);
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new QuarteringService(quarterings, shifts, studies, institutions, categories, alerts, clock);
        densityService = new DensityService(densities, shifts, studies, clock);

        List<LocalDate> dates = new ArrayList<>();
        IntStream.range(0, 8).forEach(i -> dates.add(LocalDate.of(2026, 11, 16).plusDays(i)));
        when(studies.findById(7L)).thenReturn(Optional.of(
                CharacterizationStudy.restore(7L, 1L, StudyStatus.EN_EJECUCION, dates, List.of())));
        when(institutions.findById(1L)).thenReturn(Optional.of(new Institution(1L, "I.E.", "V")));
        when(categories.findAllAsMap()).thenReturn(Map.of(
                PAPER, new WasteCategory(PAPER, "Papel", WasteGroup.BLANCO, ""),
                FOOD, new WasteCategory(FOOD, "Restos de comida", WasteGroup.VERDE, ""),
                BATTERIES, new WasteCategory(BATTERIES, "Pilas", WasteGroup.SIN_COLOR, "")));

        shift = new SamplingShift(30L, 7L, 3, ShiftType.MANANA);
        shift.registerPopulation(new ShiftPopulation(null, 300, 12, 6, new VerificationInfo(11L, NOW)));
        IntStream.range(0, 6).forEach(i -> shift.addBag(new WasteBag(null, 1L, "AU", new BigDecimal("12.40"),
                new BigDecimal("0.40"), new BigDecimal("0.080"), new VerificationInfo(11L, NOW))));
        when(shifts.findById(30L)).thenReturn(Optional.of(shift));
        when(quarterings.save(any())).thenAnswer(call -> {
            Quartering quartering = call.getArgument(0);
            if (quartering.getId() == null) {
                quartering.setId(40L);
            }
            return quartering;
        });
        when(densities.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void ca052_quarteringWithinTwoPercentIsConforming() {
        Quartering quartering = register(student, "50.00", "30.00", "19.50");

        assertEquals(QualityStatus.CONFORME, quartering.getQualityStatus());
        assertEquals(0, new BigDecimal("72.00").compareTo(quartering.getShiftNetWeight()));
        verify(alerts, never()).save(any());
    }

    @Test
    void ca052_quarteringOutsideTheToleranceIsSavedWithItsAlert() {
        Quartering quartering = register(student, "50.00", "30.00", "17.00");

        assertEquals(QualityStatus.NO_CONFORME, quartering.getQualityStatus());
        ArgumentCaptor<Alert> alert = ArgumentCaptor.forClass(Alert.class);
        verify(alerts).save(alert.capture());
        assertEquals(AlertType.CUARTEO_NO_CONFORME, alert.getValue().getType());
    }

    @Test
    void ca050_sampleOutsideTheRangeIsRejected() {
        assertThrows(BusinessRuleException.class, () -> register(student, "40.00", "20.00", "20.00"));
    }

    @Test
    void ca056_studentRegistersHazardousWasteBecauseItsRoleHasThePermission() {
        Quartering quartering = service.register(student, 30L, new BigDecimal("50.00"), List.of(
                new ResultInput(PAPER, new BigDecimal("30.00")), new ResultInput(FOOD, new BigDecimal("19.50")),
                new ResultInput(BATTERIES, new BigDecimal("0.40"))));

        assertEquals(QualityStatus.CONFORME, quartering.getQualityStatus());
    }

    @Test
    void rn055_unknownOrRepeatedSubcategoryIsRejected() {
        assertThrows(BusinessRuleException.class, () -> service.register(student, 30L, new BigDecimal("50.00"),
                List.of(new ResultInput(99L, new BigDecimal("50.00")))));
        assertThrows(BusinessRuleException.class, () -> service.register(student, 30L, new BigDecimal("50.00"),
                List.of(new ResultInput(PAPER, new BigDecimal("25.00")), new ResultInput(PAPER, new BigDecimal("25.00")))));
    }

    @Test
    void rn050_secondQuarteringOfTheShiftIsRejected() {
        register(student, "50.00", "30.00", "19.50");

        assertThrows(BusinessRuleException.class, () -> register(student, "50.00", "30.00", "19.50"));
    }

    @Test
    void rn052_onlyANonConformingQuarteringIsAnnulledWithAReason() {
        Quartering conforming = register(student, "50.00", "30.00", "19.50");
        when(quarterings.findByShift(30L)).thenReturn(Optional.of(conforming));
        assertThrows(BusinessRuleException.class, () -> service.annul(teacher, 30L, "Error de báscula"));
    }

    @Test
    void rn052_nonConformingQuarteringIsAnnulledWithWhoAndWhen() {
        Quartering nonConforming = register(student, "50.00", "30.00", "17.00");
        when(quarterings.findByShift(30L)).thenReturn(Optional.of(nonConforming));

        assertThrows(InvalidDataException.class, () -> service.annul(teacher, 30L, " "));
        Quartering annulled = service.annul(teacher, 30L, "La báscula estaba descalibrada");

        assertEquals(QualityStatus.ANULADO, annulled.getQualityStatus());
        assertEquals(10L, annulled.getAnnulledBy());
        assertEquals(NOW, annulled.getAnnulledAt());
    }

    @Test
    void rn081_inactiveUserCannotRegisterTheQuartering() {
        User inactive = new User(12L, "Ana", "Ruiz", "a@unicesar.edu.co", UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
        inactive.deactivate();

        assertThrows(AccessDeniedException.class, () -> register(inactive, "50.00", "30.00", "19.50"));
        verify(quarterings, never()).save(any());
    }

    @Test
    void ca057_densityOfTheShiftIsCalculatedAndOnlyOnce() {
        DensityMeasurement measurement = densityService.register(student, 30L, new BigDecimal("0.200"),
                new BigDecimal("30.00"));

        assertEquals(0, new BigDecimal("150.00").compareTo(measurement.getDensity()));
        assertThrows(BusinessRuleException.class,
                () -> densityService.register(student, 30L, new BigDecimal("0.200"), new BigDecimal("30.00")));
    }

    @Test
    void rn057_densityWithZeroVolumeIsInvalid() {
        assertThrows(InvalidDataException.class,
                () -> densityService.register(student, 30L, BigDecimal.ZERO, new BigDecimal("30.00")));
    }

    private Quartering register(User actor, String sample, String paper, String food) {
        return service.register(actor, 30L, new BigDecimal(sample), List.of(
                new ResultInput(PAPER, new BigDecimal(paper)), new ResultInput(FOOD, new BigDecimal(food))));
    }
}
