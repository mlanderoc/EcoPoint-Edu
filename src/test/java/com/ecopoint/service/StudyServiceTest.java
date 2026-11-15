package com.ecopoint.service;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.QualityControlException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.Institution;
import com.ecopoint.model.SourceType;
import com.ecopoint.model.StudyStatus;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.persistence.GenerationSourceRepository;
import com.ecopoint.persistence.InstitutionRepository;
import com.ecopoint.persistence.StudyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Studies, their dates and their status (RN-010 to RN-017, R-330 to R-332).
 *
 * @author EstefaniaMarquez
 */
class StudyServiceTest {

    private static final Long INSTITUTION = 1L;
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 16);

    private StudyRepository studies;
    private GenerationSourceRepository sources;
    private InstitutionRepository institutions;
    private StudyService service;
    private final User teacher = new User(10L, "Daniela", "Arias", "d@unicesar.edu.co", UserRole.DOCENTE, INSTITUTION);
    private final User student = new User(11L, "Laura", "Pérez", "l@unicesar.edu.co",
            UserRole.ESTUDIANTE_UNIVERSITARIO, INSTITUTION);
    private final List<CharacterizationStudy> existing = new ArrayList<>();

    @BeforeEach
    void setUp() {
        studies = mock(StudyRepository.class);
        sources = mock(GenerationSourceRepository.class);
        institutions = mock(InstitutionRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-11-12T12:00:00Z"), ZoneOffset.UTC);
        service = new StudyService(studies, institutions, sources, clock);

        when(institutions.findById(INSTITUTION)).thenReturn(Optional.of(new Institution(INSTITUTION, "I.E.", "V")));
        when(studies.findByInstitution(INSTITUTION)).thenReturn(existing);
        when(studies.save(any())).thenAnswer(call -> {
            CharacterizationStudy study = call.getArgument(0);
            if (study.getId() == null) {
                study.setId(7L);
            }
            return study;
        });
        GenerationSource active = new GenerationSource(1L, INSTITUTION, "AU1", "Aulas 1", SourceType.AULAS);
        GenerationSource inactive = new GenerationSource(2L, INSTITUTION, "LAB", "Laboratorio", SourceType.LABORATORIO);
        inactive.deactivate();
        when(sources.findByInstitution(INSTITUTION)).thenReturn(List.of(active, inactive));
    }

    @Test
    void r331_newStudyIsPlannedWithAllActiveSources() {
        CharacterizationStudy created = service.create(teacher, INSTITUTION, eightDays(MONDAY));

        assertEquals(StudyStatus.PLANIFICADO, created.getStatus());
        assertEquals(MONDAY, created.getStartDate());
        verify(studies).saveParticipatingSources(7L, Set.of(1L));
    }

    @Test
    void ca203_studentCannotCreateAStudy() {
        assertThrows(AccessDeniedException.class, () -> service.create(student, INSTITUTION, eightDays(MONDAY)));
        verify(studies, never()).save(any());
    }

    @Test
    void rn011_datesOutOfOrderAreRejected() {
        List<LocalDate> dates = eightDays(MONDAY);
        dates.set(3, MONDAY);

        assertThrows(InvalidDataException.class, () -> service.create(teacher, INSTITUTION, dates));
    }

    @Test
    void rn013_studyThatOverlapsAnotherOfTheInstitutionIsRejected() {
        CharacterizationStudy other = new CharacterizationStudy(3L, INSTITUTION);
        other.scheduleDates(eightDays(MONDAY));
        existing.add(other);

        assertThrows(BusinessRuleException.class,
                () -> service.create(teacher, INSTITUTION, eightDays(MONDAY.plusDays(2))));
    }

    @Test
    void ca013_studyCannotSkipAStatus() {
        stored(StudyStatus.PLANIFICADO);

        assertThrows(BusinessRuleException.class, () -> service.changeStatus(teacher, 7L, StudyStatus.CERRADO, null));
    }

    @Test
    void rn015_studyStartsOnlyWithAnActiveParticipatingSource() {
        stored(StudyStatus.PLANIFICADO);
        when(studies.findParticipatingSources(7L)).thenReturn(Set.of(2L));

        assertThrows(BusinessRuleException.class,
                () -> service.changeStatus(teacher, 7L, StudyStatus.EN_EJECUCION, null));

        when(studies.findParticipatingSources(7L)).thenReturn(Set.of(1L, 2L));
        assertEquals(StudyStatus.EN_EJECUCION,
                service.changeStatus(teacher, 7L, StudyStatus.EN_EJECUCION, null).getStatus());
    }

    @Test
    void ca014_studyWithPendingRecordsCannotBeClosed() {
        stored(StudyStatus.EN_DEPURACION);
        when(studies.countPendingRecords(7L)).thenReturn(4);

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.changeStatus(teacher, 7L, StudyStatus.CERRADO, null));
        assertEquals("No se puede cerrar el estudio: quedan 4 registros pendientes de verificación.",
                error.getMessage());
    }

    @Test
    void rn016_studyWithUnresolvedQuarteringCannotBeClosed() {
        stored(StudyStatus.EN_DEPURACION);
        when(studies.countUnresolvedQuarterings(7L)).thenReturn(1);

        assertThrows(QualityControlException.class, () -> service.changeStatus(teacher, 7L, StudyStatus.CERRADO, null));
    }

    @Test
    void rn017_closedStudyIsReopenedWithReasonAndDate() {
        stored(StudyStatus.CERRADO);

        assertThrows(InvalidDataException.class,
                () -> service.changeStatus(teacher, 7L, StudyStatus.EN_DEPURACION, " "));

        CharacterizationStudy reopened = service.changeStatus(teacher, 7L, StudyStatus.EN_DEPURACION,
                "Error de digitación en el día 4");
        assertEquals(StudyStatus.EN_DEPURACION, reopened.getStatus());
        assertEquals(LocalDateTime.of(2026, 11, 12, 12, 0), reopened.getReopenings().get(0).at());
        assertEquals(10L, reopened.getReopenings().get(0).userId());
    }

    @Test
    void r331_participatingSourcesMustBelongToTheInstitution() {
        stored(StudyStatus.PLANIFICADO);

        assertThrows(InvalidDataException.class, () -> service.setParticipatingSources(teacher, 7L, Set.of(1L, 99L)));
        assertEquals(Set.of(1L), service.setParticipatingSources(teacher, 7L, Set.of(1L)));
    }

    @Test
    void r330_closedStudyCannotBeRescheduled() {
        stored(StudyStatus.CERRADO);

        assertThrows(BusinessRuleException.class, () -> service.reschedule(teacher, 7L, eightDays(MONDAY)));
    }

    @Test
    void ca207_missingStudyIsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.get(99L));
    }

    private void stored(StudyStatus status) {
        CharacterizationStudy study = CharacterizationStudy.restore(7L, INSTITUTION, status, eightDays(MONDAY),
                List.of());
        when(studies.findById(7L)).thenReturn(Optional.of(study));
    }

    private static List<LocalDate> eightDays(LocalDate first) {
        List<LocalDate> dates = new ArrayList<>();
        IntStream.range(0, 8).forEach(i -> dates.add(first.plusDays(i)));
        return dates;
    }

    @Test
    void ca015_executingStudyIsCancelledWithReasonDateAndTeacher() {
        stored(StudyStatus.EN_EJECUCION);

        assertThrows(InvalidDataException.class, () -> service.cancel(teacher, 7L, ""));
        CharacterizationStudy cancelled = service.cancel(teacher, 7L, "Fechas equivocadas");

        assertEquals(StudyStatus.CANCELADO, cancelled.getStatus());
        assertEquals(10L, cancelled.getCancellation().userId());
        assertEquals(LocalDateTime.of(2026, 11, 12, 12, 0), cancelled.getCancellation().at());
        assertThrows(AccessDeniedException.class, () -> service.cancel(student, 7L, "Motivo"));
    }

    @Test
    void ca016_closedStudyCannotBeCancelledNorRescheduledWhenCancelled() {
        stored(StudyStatus.CERRADO);
        assertThrows(BusinessRuleException.class, () -> service.cancel(teacher, 7L, "Motivo"));

        stored(StudyStatus.CANCELADO);
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.reschedule(teacher, 7L, eightDays(MONDAY)));
        assertTrue(error.getMessage().contains("cancelado"));
    }

    @Test
    void ca017_onlyPlannedStudyIsDeleted() {
        stored(StudyStatus.EN_EJECUCION);
        assertThrows(BusinessRuleException.class, () -> service.delete(teacher, 7L));
        verify(studies, never()).delete(7L);

        stored(StudyStatus.PLANIFICADO);
        service.delete(teacher, 7L);
        verify(studies).delete(7L);
        assertThrows(AccessDeniedException.class, () -> service.delete(student, 7L));
    }

    @Test
    void ca005_inactiveInstitutionDoesNotAcceptNewStudies() {
        Institution inactive = new Institution(INSTITUTION, "I.E.", "V");
        inactive.deactivate();
        when(institutions.findById(INSTITUTION)).thenReturn(Optional.of(inactive));

        assertThrows(BusinessRuleException.class, () -> service.create(teacher, INSTITUTION, eightDays(MONDAY)));
        verify(studies, never()).save(any());
    }
}
