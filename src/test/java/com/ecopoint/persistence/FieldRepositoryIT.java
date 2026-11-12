package com.ecopoint.persistence;

import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.Crew;
import com.ecopoint.model.DensityMeasurement;
import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.Institution;
import com.ecopoint.model.QualityStatus;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.QuarteringResult;
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
import com.ecopoint.model.WasteCategory;
import com.ecopoint.model.WasteGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests of the field repositories against PostgreSQL: shifts,
 * bags, quartering and density (CA-103, CA-105, CA-106, CA-110).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class FieldRepositoryIT extends PostgresIntegrationTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 3, 4, 12, 0);

    @Autowired private ShiftRepository shifts;
    @Autowired private WasteBagRepository bags;
    @Autowired private QuarteringRepository quarterings;
    @Autowired private DensityRepository densities;
    @Autowired private StudyRepository studies;
    @Autowired private InstitutionRepository institutions;
    @Autowired private GenerationSourceRepository sources;
    @Autowired private UserRepository users;
    @Autowired private WasteCategoryRepository categories;
    @Autowired private CrewRepository crews;

    private Long studyId;
    private Long studentId;
    private GenerationSource classrooms;
    private GenerationSource lab;
    private WasteCategory paper;
    private WasteCategory food;

    @BeforeEach
    void createFixture() {
        Long institution = institutions.save(new Institution(null, "I.E. Prudencia Daza", "Valledupar")).getId();
        studentId = users.save(new User(null, "Laura", "Ríos", "laura@unicesar.edu.co",
                UserRole.ESTUDIANTE_UNIVERSITARIO, institution)).getId();
        classrooms = sources.save(new GenerationSource(null, institution, "AU1", "Aulas 1", SourceType.AULAS));
        lab = sources.save(new GenerationSource(null, institution, "LAB", "Laboratorio", SourceType.LABORATORIO));
        paper = categories.save(new WasteCategory(null, "Papel", WasteGroup.BLANCO, null));
        food = categories.save(new WasteCategory(null, "Restos de comida", WasteGroup.VERDE, null));
        CharacterizationStudy study = new CharacterizationStudy(null, institution);
        study.scheduleDates(List.of(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 4),
                LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 6), LocalDate.of(2026, 3, 9),
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 11)));
        study.changeStatus(StudyStatus.EN_EJECUCION);
        studyId = studies.save(study).getId();
    }

    private VerificationInfo typedByStudent() {
        return new VerificationInfo(studentId, AT);
    }

    private SamplingShift openShiftWithPopulation() {
        SamplingShift shift = new SamplingShift(null, studyId, 3, ShiftType.MANANA);
        shift.registerPopulation(new ShiftPopulation(null, 480, 28, 15, typedByStudent()));
        return shifts.save(shift);
    }

    private WasteBag bag(GenerationSource source, String gross, String tare, String volume) {
        return new WasteBag(null, source.getId(), source.getCode(), new BigDecimal(gross), new BigDecimal(tare),
                new BigDecimal(volume), typedByStudent());
    }

    @Test
    void ca106_bagIsStoredAndTheDatabaseCalculatesNetWeightAndDensity() {
        SamplingShift shift = openShiftWithPopulation();
        WasteBag bag = bag(classrooms, "12.40", "0.40", "0.080");
        shift.addBag(bag);

        bags.save(shift.getId(), bag);

        assertEquals("AU1-D3-M", bags.findById(bag.getId()).orElseThrow().getLabelText());
        assertEquals(new BigDecimal("12.00"),
                jdbc.queryForObject("SELECT peso_neto FROM BOLSA WHERE id = ?", BigDecimal.class, bag.getId()));
        assertEquals(new BigDecimal("150.00"),
                jdbc.queryForObject("SELECT densidad FROM BOLSA WHERE id = ?", BigDecimal.class, bag.getId()));
    }

    @Test
    void ca110_shiftIsRebuiltWithPopulationBagsMarksQuarteringAndDensity() {
        SamplingShift shift = openShiftWithPopulation();
        WasteBag first = bag(classrooms, "30.40", "0.40", "0.200");
        WasteBag second = bag(classrooms, "25.20", "0.20", "0.180");
        shift.addBag(first);
        shift.addBag(second);
        bags.save(shift.getId(), first);
        bags.save(shift.getId(), second);
        shift.markSourceWithoutGeneration(lab.getId());
        shifts.save(shift);
        Quartering quartering = new Quartering(null, shift.getId(), shift.calculateTotalNetWeight(),
                new BigDecimal("50.00"), typedByStudent());
        quartering.addResult(QuarteringResult.of(paper, new BigDecimal("20.00")));
        quartering.addResult(QuarteringResult.of(food, new BigDecimal("29.60")));
        quartering.evaluateQuality(new BigDecimal("2"));
        quarterings.save(quartering);
        densities.save(new DensityMeasurement(null, shift.getId(), new BigDecimal("0.208"), new BigDecimal("31.20"),
                typedByStudent()));
        shifts.updateStatus(shift.getId(), ShiftStatus.CERRADO);

        SamplingShift found = shifts.findById(shift.getId()).orElseThrow();

        assertFalse(found.isOpen());
        assertEquals(523, found.getPopulation().getTotal());
        assertEquals(List.of("AU1-D3-M", "AU1-D3-M-2"), found.getBags().stream().map(WasteBag::getLabelText).toList());
        assertTrue(found.isSourceCovered(lab.getId()));
        assertEquals(new BigDecimal("55.00"), found.calculateTotalNetWeight());
        assertEquals(QualityStatus.CONFORME, found.getQuartering().getQualityStatus());
        assertEquals(2, found.getQuartering().getResults().size());
        assertEquals(new BigDecimal("150.00"), found.getDensity().getDensity());
        assertNotNull(jdbc.queryForObject("SELECT cerrado_en FROM TURNO WHERE id = ?", Object.class, shift.getId()));
        assertEquals(1, shifts.findByStudy(studyId).size());
    }

    @Test
    void ca105_quarteringWithAnInvalidResultIsNotStoredAtAll() {
        SamplingShift shift = openShiftWithPopulation();
        Quartering quartering = new Quartering(null, shift.getId(), new BigDecimal("60.00"), new BigDecimal("50.00"),
                typedByStudent());
        quartering.addResult(QuarteringResult.of(paper, new BigDecimal("20.00")));
        quartering.addResult(QuarteringResult.of(food, new BigDecimal("15.00")));
        quartering.addResult(new QuarteringResult(paper.getId(), "Papel", WasteGroup.BLANCO, new BigDecimal("1.00")));
        quartering.addResult(new QuarteringResult(food.getId() + 99, "No existe", WasteGroup.NEGRO, BigDecimal.ONE));
        quartering.addResult(QuarteringResult.of(food, new BigDecimal("-5.00")));
        quartering.evaluateQuality(new BigDecimal("2"));

        assertThrows(PersistenceException.class, () -> quarterings.save(quartering));

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM CUARTEO", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM RESULTADO_CUARTEO", Integer.class));
    }

    @Test
    void ca103_secondQuarteringForTheSameShiftIsRejected() {
        SamplingShift shift = openShiftWithPopulation();
        Quartering first = new Quartering(null, shift.getId(), new BigDecimal("60.00"), new BigDecimal("50.00"),
                typedByStudent());
        first.addResult(QuarteringResult.of(paper, new BigDecimal("49.50")));
        first.evaluateQuality(new BigDecimal("2"));
        quarterings.save(first);
        Quartering second = new Quartering(null, shift.getId(), new BigDecimal("60.00"), new BigDecimal("50.00"),
                typedByStudent());
        second.addResult(QuarteringResult.of(paper, new BigDecimal("49.50")));
        second.evaluateQuality(new BigDecimal("2"));

        assertThrows(PersistenceException.class, () -> quarterings.save(second));
    }

    @Test
    void ca110_quarteringUpdateReplacesResultsAndStoresAnnulment() {
        SamplingShift shift = openShiftWithPopulation();
        Quartering quartering = new Quartering(null, shift.getId(), new BigDecimal("60.00"), new BigDecimal("50.00"),
                typedByStudent());
        quartering.addResult(QuarteringResult.of(paper, new BigDecimal("47.00")));
        quartering.evaluateQuality(new BigDecimal("2"));
        quarterings.save(quartering);

        quartering.annul("Muestra mojada por lluvia", studentId, AT.plusHours(1));
        quarterings.save(quartering);

        Quartering found = quarterings.findById(quartering.getId()).orElseThrow();
        assertEquals(QualityStatus.ANULADO, found.getQualityStatus());
        assertEquals("Muestra mojada por lluvia", found.getAnnulReason());
        assertEquals(1, found.getResults().size());
    }

    @Test
    void ca110_seedShiftsAreReadBackCompletely() {
        runScript("002_seed.sql");
        Long closed = jdbc.queryForObject("SELECT id FROM ESTUDIO WHERE estado = 'CERRADO'", Long.class);

        List<SamplingShift> list = shifts.findByStudy(closed);

        assertEquals(16, list.size());
        assertTrue(list.stream().allMatch(SamplingShift::hasQuartering));
        assertTrue(list.stream().allMatch(SamplingShift::hasDensity));
        assertEquals(1, list.stream().filter(s -> s.getQuartering().getQualityStatus() == QualityStatus.ANULADO).count());
    }

    @Test
    void r341_shiftKeepsTheCrewThatWorksIt() {
        User teacher = users.save(new User(null, "Oriana", "Jurado", "oriana.crew@unicesar.edu.co", UserRole.DOCENTE,
                institutions.findAll().get(0).getId()));
        Crew crew = crews.save(new Crew(null, "Cuadrilla A", teacher));
        SamplingShift shift = new SamplingShift(null, studyId, 4, ShiftType.TARDE);
        shift.assignCrew(crew.getId());
        shifts.save(shift);

        assertEquals(crew.getId(), shifts.findById(shift.getId()).orElseThrow().getCrewId());

        shift.assignCrew(null);
        shifts.save(shift);
        assertNull(shifts.findById(shift.getId()).orElseThrow().getCrewId());
    }
}
