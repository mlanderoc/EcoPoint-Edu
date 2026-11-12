package com.ecopoint.persistence;

import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.Institution;
import com.ecopoint.model.StudyStatus;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests of {@link StudyRepository} against PostgreSQL
 * (CA-110, RN-010, RN-016, RN-017).
 *
 * @author mlanderoc
 * @version 1.0
 */
class StudyRepositoryIT extends PostgresIntegrationTest {

    @Autowired
    private StudyRepository repository;

    @Autowired
    private InstitutionRepository institutions;

    @Autowired
    private UserRepository users;

    private Long institutionId;

    @BeforeEach
    void createInstitution() {
        institutionId = institutions.save(new Institution(null, "I.E. Prudencia Daza", "Valledupar")).getId();
    }

    private static List<LocalDate> march(int... days) {
        return Arrays.stream(days).mapToObj(day -> LocalDate.of(2026, 3, day)).toList();
    }

    @Test
    void ca110_newStudyIsSavedWithItsEightDays() {
        CharacterizationStudy study = new CharacterizationStudy(null, institutionId);
        study.scheduleDates(march(2, 3, 4, 5, 6, 9, 10, 11));

        repository.save(study);

        CharacterizationStudy found = repository.findById(study.getId()).orElseThrow();
        assertEquals(StudyStatus.PLANIFICADO, found.getStatus());
        assertEquals(8, found.getDays().size());
        assertEquals(LocalDate.of(2026, 3, 2), found.getStartDate());
        assertEquals(LocalDate.of(2026, 3, 11), found.getEndDate());
        assertTrue(found.isDayExcluded(1));
    }

    @Test
    void ca110_planningWithoutDatesAndReschedulingShiftsTheDates() {
        CharacterizationStudy study = repository.save(new CharacterizationStudy(null, institutionId));
        assertEquals(null, repository.findById(study.getId()).orElseThrow().getStartDate());

        study.scheduleDates(march(2, 3, 4, 5, 6, 9, 10, 11));
        repository.save(study);
        study.scheduleDates(march(3, 4, 5, 6, 9, 10, 11, 12));
        repository.save(study);

        CharacterizationStudy found = repository.findById(study.getId()).orElseThrow();
        assertEquals(LocalDate.of(2026, 3, 3), found.getStartDate());
        assertEquals(LocalDate.of(2026, 3, 12), found.getEndDate());
    }

    @Test
    void rn017_statusAndReopeningsAreStoredAndRestored() {
        User teacher = users.save(new User(null, "Oriana", "Jurado", "oriana@unicesar.edu.co", UserRole.DOCENTE, institutionId));
        CharacterizationStudy study = new CharacterizationStudy(null, institutionId);
        study.scheduleDates(march(2, 3, 4, 5, 6, 9, 10, 11));
        repository.save(study);
        study.changeStatus(StudyStatus.EN_EJECUCION);
        study.changeStatus(StudyStatus.EN_DEPURACION);
        study.changeStatus(StudyStatus.CERRADO);
        repository.save(study);

        study.reopen(teacher.getId(), "Corregir el pesaje del día 4", LocalDateTime.of(2026, 3, 20, 10, 0));
        repository.save(study);
        repository.save(study);

        CharacterizationStudy found = repository.findById(study.getId()).orElseThrow();
        assertEquals(StudyStatus.EN_DEPURACION, found.getStatus());
        assertEquals(1, found.getReopenings().size());
        assertEquals("Corregir el pesaje del día 4", found.getReopenings().get(0).reason());
    }

    @Test
    void ca110_updateStatusListAndParticipatingSources() {
        CharacterizationStudy first = repository.save(new CharacterizationStudy(null, institutionId));
        CharacterizationStudy second = repository.save(new CharacterizationStudy(null, institutionId));

        repository.updateStatus(first.getId(), StudyStatus.EN_EJECUCION);

        assertEquals(StudyStatus.EN_EJECUCION, repository.findById(first.getId()).orElseThrow().getStatus());
        assertEquals(List.of(second.getId(), first.getId()),
                repository.findByInstitution(institutionId).stream().map(CharacterizationStudy::getId).toList());
    }

    @Test
    void rn016_pendingRecordsAndUnresolvedQuarteringsAreCounted() {
        runScript("002_seed.sql");
        Long closed = jdbc.queryForObject("SELECT id FROM ESTUDIO WHERE estado = 'CERRADO'", Long.class);
        Long executing = jdbc.queryForObject("SELECT id FROM ESTUDIO WHERE estado = 'EN_EJECUCION'", Long.class);

        assertEquals(0, repository.countPendingRecords(closed));
        assertEquals(16, repository.countPendingRecords(executing));
        assertEquals(0, repository.countUnresolvedQuarterings(closed));

        jdbc.update("UPDATE CUARTEO SET estado_calidad = 'NO_CONFORME' WHERE id = (SELECT MIN(c.id) FROM CUARTEO c "
                + "JOIN TURNO t ON t.id = c.id_turno WHERE t.id_estudio = ?)", executing);
        assertEquals(1, repository.countUnresolvedQuarterings(executing));
        assertEquals(8, repository.findParticipatingSources(closed).size());

        repository.saveParticipatingSources(executing, Set.of(1L, 2L));
        assertEquals(Set.of(1L, 2L), repository.findParticipatingSources(executing));
    }
}
