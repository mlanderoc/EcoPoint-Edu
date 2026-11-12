package com.ecopoint.service;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftPopulation;
import com.ecopoint.model.ShiftStatus;
import com.ecopoint.model.ShiftType;
import com.ecopoint.model.SourceType;
import com.ecopoint.model.StudyStatus;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.model.VerificationInfo;
import com.ecopoint.model.WasteBag;
import com.ecopoint.persistence.CrewRepository;
import com.ecopoint.persistence.GenerationSourceRepository;
import com.ecopoint.persistence.ShiftRepository;
import com.ecopoint.persistence.StudyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Shifts: open, close, reopen, population and "no generation" (RN-020 to RN-025, R-340 to R-346).
 *
 * @author EstefaniaMarquez
 */
class ShiftServiceTest {

    private static final Long STUDY = 7L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 7, 30);

    private ShiftRepository shifts;
    private StudyRepository studies;
    private ShiftService service;
    private final User teacher = new User(10L, "Daniela", "Arias", "d@unicesar.edu.co", UserRole.DOCENTE, 1L);
    private final User student = new User(11L, "Laura", "Pérez", "l@unicesar.edu.co",
            UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
    private final GenerationSource classrooms = new GenerationSource(1L, 1L, "AU1", "Aulas 1", SourceType.AULAS);
    private final GenerationSource store = new GenerationSource(2L, 1L, "TI", "Tienda", SourceType.TIENDA_ESCOLAR);
    private final List<SamplingShift> opened = new ArrayList<>();

    @BeforeEach
    void setUp() {
        shifts = mock(ShiftRepository.class);
        studies = mock(StudyRepository.class);
        GenerationSourceRepository sources = mock(GenerationSourceRepository.class);
        CrewRepository crews = mock(CrewRepository.class);
        service = new ShiftService(shifts, studies, sources, crews,
                Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

        study(StudyStatus.EN_EJECUCION);
        when(studies.findParticipatingSources(STUDY)).thenReturn(Set.of(1L, 2L));
        when(sources.findByInstitution(1L)).thenReturn(List.of(classrooms, store));
        when(shifts.findByStudy(STUDY)).thenReturn(opened);
        when(shifts.save(any())).thenAnswer(call -> {
            SamplingShift shift = call.getArgument(0);
            if (shift.getId() == null) {
                shift.setId(30L);
            }
            return shift;
        });
        when(crews.findById(99L)).thenReturn(Optional.empty());
    }

    @Test
    void r341_teacherOpensAShiftOfAStudyInExecution() {
        SamplingShift shift = service.open(teacher, STUDY, 3, ShiftType.MANANA, null);

        assertEquals(ShiftStatus.ABIERTO, shift.getStatus());
        assertEquals("D3-M", shift.getCode());
    }

    @Test
    void ca204_studentCannotOpenOrCloseShifts() {
        assertThrows(AccessDeniedException.class, () -> service.open(student, STUDY, 3, ShiftType.MANANA, null));
        stored(fullShift());
        assertThrows(AccessDeniedException.class, () -> service.close(student, 30L));
    }

    @Test
    void rn020_secondShiftOfTheSameDayAndTypeIsRejected() {
        opened.add(new SamplingShift(30L, STUDY, 3, ShiftType.MANANA));

        assertThrows(BusinessRuleException.class, () -> service.open(teacher, STUDY, 3, ShiftType.MANANA, null));
    }

    @Test
    void rn020_shiftsOnlyOpenWhileTheStudyIsInExecution() {
        study(StudyStatus.PLANIFICADO);

        assertThrows(BusinessRuleException.class, () -> service.open(teacher, STUDY, 3, ShiftType.MANANA, null));
    }

    @Test
    void r341_unknownCrewIsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.open(teacher, STUDY, 3, ShiftType.MANANA, 99L));
    }

    @Test
    void rn022_populationIsRegisteredWithWhoTypedItAndWhen() {
        stored(new SamplingShift(30L, STUDY, 3, ShiftType.MANANA));

        SamplingShift shift = service.registerPopulation(student, 30L, 300, 12, 6);

        assertEquals(318, shift.getPopulation().getTotal());
        assertEquals(11L, shift.getPopulation().getVerification().getEnteredBy());
        assertEquals(NOW, shift.getPopulation().getVerification().getEnteredAt());
    }

    @Test
    void ca021_populationOfZeroPeopleIsInvalid() {
        stored(new SamplingShift(30L, STUDY, 3, ShiftType.MANANA));

        assertThrows(InvalidDataException.class, () -> service.registerPopulation(student, 30L, 0, 0, 0));
    }

    @Test
    void rn024_shiftWithPendingSourcesCannotBeClosed() {
        SamplingShift shift = new SamplingShift(30L, STUDY, 3, ShiftType.MANANA);
        withPopulation(shift);
        shift.addBag(bag(classrooms));
        stored(shift);

        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> service.close(teacher, 30L));
        assertTrue(error.getMessage().contains("TI"));
    }

    @Test
    void rn024_shiftClosesWhenEverySourceHasBagsOrNoGeneration() {
        stored(fullShift());

        assertEquals(ShiftStatus.CERRADO, service.close(teacher, 30L).getStatus());
    }

    @Test
    void r345_progressShowsEachSourceWithItsState() {
        SamplingShift shift = new SamplingShift(30L, STUDY, 3, ShiftType.MANANA);
        withPopulation(shift);
        shift.addBag(bag(classrooms));

        List<SourceProgress> progress = service.progress(shift);

        assertEquals(SourceShiftStatus.CON_GENERACION, progress.get(0).status());
        assertEquals(new BigDecimal("12.00"), progress.get(0).totalNetWeight());
        assertEquals(SourceShiftStatus.PENDIENTE, progress.get(1).status());
    }

    @Test
    void rn024_onlyParticipatingSourcesCanBeMarkedWithoutGeneration() {
        stored(new SamplingShift(30L, STUDY, 3, ShiftType.MANANA));

        assertThrows(BusinessRuleException.class, () -> service.setNoGeneration(student, 30L, 55L, true));

        SamplingShift shift = service.setNoGeneration(student, 30L, 2L, true);
        assertTrue(shift.getSourcesWithoutGeneration().contains(2L));
        assertTrue(service.setNoGeneration(student, 30L, 2L, false).getSourcesWithoutGeneration().isEmpty());
    }

    @Test
    void rn025_closedShiftIsReopenedOnlyWhileTheStudyIsInExecution() {
        SamplingShift shift = fullShift();
        shift.close();
        stored(shift);

        assertEquals(ShiftStatus.ABIERTO, service.reopen(teacher, 30L).getStatus());
    }

    @Test
    void r340_studyHasSixteenSlotsWithTheirProgress() {
        SamplingShift shift = new SamplingShift(30L, STUDY, 2, ShiftType.TARDE);
        withPopulation(shift);
        shift.addBag(bag(classrooms));
        opened.add(shift);

        List<ShiftSlot> slots = service.slots(STUDY);

        assertEquals(16, slots.size());
        assertNull(slots.get(0).shift());
        ShiftSlot dayTwoAfternoon = slots.get(3);
        assertEquals(2, dayTwoAfternoon.dayNumber());
        assertEquals(ShiftType.TARDE, dayTwoAfternoon.type());
        assertEquals(1, dayTwoAfternoon.coveredSources());
        assertEquals(2, dayTwoAfternoon.totalSources());
    }

    private SamplingShift fullShift() {
        SamplingShift shift = new SamplingShift(30L, STUDY, 3, ShiftType.MANANA);
        withPopulation(shift);
        shift.addBag(bag(classrooms));
        shift.markSourceWithoutGeneration(2L);
        return shift;
    }

    private void withPopulation(SamplingShift shift) {
        shift.registerPopulation(new ShiftPopulation(null, 300, 12, 6, new VerificationInfo(11L, NOW)));
    }

    private static WasteBag bag(GenerationSource source) {
        return new WasteBag(null, source.getId(), source.getCode(), new BigDecimal("12.40"), new BigDecimal("0.40"),
                new BigDecimal("0.080"), new VerificationInfo(11L, NOW));
    }

    private void stored(SamplingShift shift) {
        when(shifts.findById(30L)).thenReturn(Optional.of(shift));
    }

    private void study(StudyStatus status) {
        List<LocalDate> dates = new ArrayList<>();
        IntStream.range(0, 8).forEach(i -> dates.add(LocalDate.of(2026, 11, 16).plusDays(i)));
        when(studies.findById(STUDY)).thenReturn(Optional.of(
                CharacterizationStudy.restore(STUDY, 1L, status, dates, List.of())));
    }
}
