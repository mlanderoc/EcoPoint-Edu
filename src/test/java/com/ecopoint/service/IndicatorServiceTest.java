package com.ecopoint.service;

import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.QuarteringResult;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftPopulation;
import com.ecopoint.model.ShiftType;
import com.ecopoint.model.SourceType;
import com.ecopoint.model.StudyStatus;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.model.VerificationInfo;
import com.ecopoint.model.WasteBag;
import com.ecopoint.model.WasteGroup;
import com.ecopoint.persistence.CorrectionRepository;
import com.ecopoint.persistence.GenerationSourceRepository;
import com.ecopoint.persistence.InstitutionRepository;
import com.ecopoint.persistence.ShiftRepository;
import com.ecopoint.persistence.StudyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Formulas of the baseline indicators with the data of the spec (spec 006,
 * CA-500 to CA-503, CA-509).
 *
 * @author EstefaniaMarquez
 */
class IndicatorServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 16, 0);
    /** G(d) of days 1 to 8 from the common test data of the spec. */
    private static final int[] DAILY_GENERATION = {150, 60, 62, 58, 64, 56, 60, 60};

    private final StudyRepository studies = mock(StudyRepository.class);
    private final ShiftRepository shifts = mock(ShiftRepository.class);
    private final GenerationSourceRepository sources = mock(GenerationSourceRepository.class);
    private final IndicatorService service = new IndicatorService(studies, shifts, sources,
            mock(InstitutionRepository.class), mock(CorrectionRepository.class));
    private final User teacher = new User(1L, "Ana", "Ríos", "a@unicesar.edu.co", UserRole.DOCENTE, 1L);
    private final List<SamplingShift> studyShifts = new ArrayList<>();
    private long nextId = 1;

    @BeforeEach
    void setUp() {
        study(StudyStatus.CERRADO);
        when(shifts.findByStudy(7L)).thenReturn(studyShifts);
        when(sources.findByInstitution(1L)).thenReturn(List.of(
                new GenerationSource(1L, 1L, "AU", "Aulas", SourceType.AULAS),
                new GenerationSource(2L, 1L, "TI", "Tienda escolar", SourceType.TIENDA_ESCOLAR)));
        for (int day = 1; day <= 8; day++) {
            BigDecimal half = BigDecimal.valueOf(DAILY_GENERATION[day - 1]).divide(BigDecimal.valueOf(2));
            studyShifts.add(shift(day, ShiftType.MANANA, half));
            studyShifts.add(shift(day, ShiftType.TARDE, half));
        }
    }

    @Test
    void ca500_dailyAverageGenerationExcludesDayOne() {
        StudyIndicators indicators = service.calculate(teacher, 7L);

        assertEquals("60.00", shown(indicators.summary().averageDailyGeneration(), 2));
        assertEquals(7, indicators.summary().countedDays());
        assertTrue(indicators.days().get(0).excluded());
        assertEquals("150.00", shown(indicators.days().get(0).generation(), 2));
    }

    @Test
    void ca501_ppcAverageStdDevAndCvCalculated() {
        StudyIndicators.Summary summary = service.calculate(teacher, 7L).summary();

        assertEquals("0.150", shown(summary.averagePpc(), 3));
        assertEquals("0.006455", shown(summary.ppcStandardDeviation(), 6));
        assertEquals("0.006", shown(summary.ppcStandardDeviation(), 3));
        assertEquals("4.30", shown(summary.ppcVariationCoefficient(), 2));
    }

    @Test
    void ca502_massWeightedCompositionAndRecoverableFraction() {
        quartering(studyShifts.get(2), "50", "10", "25", "15");
        quartering(studyShifts.get(3), "25", "15", "5", "5");

        StudyIndicators indicators = service.calculate(teacher, 7L);

        assertEquals("33.33", groupShare(indicators, WasteGroup.BLANCO));
        assertEquals("40.00", groupShare(indicators, WasteGroup.VERDE));
        assertEquals("26.67", groupShare(indicators, WasteGroup.NEGRO));
        assertEquals("73.33", shown(indicators.summary().recoverableFraction(), 2));
        assertEquals("44.00", shown(indicators.summary().recoverableDailyGeneration(), 2));
        assertEquals(2, indicators.composition().shiftsWithQuartering());
        assertEquals(14, indicators.composition().expectedShifts());
    }

    @Test
    void ca503_annulledQuarteringExcludedFromComposition() {
        quartering(studyShifts.get(2), "50", "10", "25", "15");
        Quartering annulled = quartering(studyShifts.get(3), "50", "30", "1", "1");
        annulled.evaluateQuality(new BigDecimal("2"));
        annulled.annul("Se perdió la submuestra", 1L, NOW);

        StudyIndicators indicators = service.calculate(teacher, 7L);

        assertEquals("20.00", groupShare(indicators, WasteGroup.BLANCO));
        assertEquals(1, indicators.composition().shiftsWithQuartering());
    }

    @Test
    void r500_dayOneQuarteringStaysOutOfTheComposition() {
        quartering(studyShifts.get(0), "50", "50", "0", "0");

        StudyIndicators indicators = service.calculate(teacher, 7L);

        assertNull(indicators.composition().groups().get(0).percentage());
        assertNull(indicators.summary().recoverableFraction());
    }

    @Test
    void r502_indicatorsArePreliminaryUntilTheStudyIsClosed() {
        assertTrue(service.calculate(teacher, 7L).official());
        study(StudyStatus.EN_DEPURACION);

        StudyIndicators indicators = service.calculate(teacher, 7L);

        assertFalse(indicators.official());
        assertEquals("0.00", shown(indicators.quality().verifiedPercentage(), 2));
    }

    @Test
    void r524_generationBySourceIsSortedFromHighest() {
        studyShifts.get(2).addBag(bag(2L, "TI", new BigDecimal("70")));

        List<StudyIndicators.SourceGeneration> bySource = service.calculate(teacher, 7L).sources();

        assertEquals("Aulas", bySource.get(0).name());
        assertEquals("60.00", shown(bySource.get(0).averageDailyGeneration(), 2));
        assertEquals("10.00", shown(bySource.get(1).averageDailyGeneration(), 2));
    }

    @Test
    void r525_morningAndAfternoonAreCompared() {
        StudyIndicators.ShiftTypeComparison morning = service.calculate(teacher, 7L).shiftTypes().get(0);

        assertEquals(ShiftType.MANANA, morning.type());
        assertEquals("30.00", shown(morning.averageGeneration(), 2));
        assertEquals("200.00", shown(morning.averagePopulation(), 2));
        assertEquals("0.150", shown(morning.averagePpc(), 3));
    }

    @Test
    void unknownStudyIsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.calculate(teacher, 99L));
    }

    private SamplingShift shift(int day, ShiftType type, BigDecimal generation) {
        SamplingShift shift = new SamplingShift(nextId++, 7L, day, type);
        shift.registerPopulation(new ShiftPopulation(nextId++, 180, 12, 8, info()));
        shift.addBag(bag(1L, "AU", generation));
        return shift;
    }

    private WasteBag bag(Long sourceId, String code, BigDecimal netWeight) {
        return new WasteBag(nextId++, sourceId, code, netWeight, BigDecimal.ZERO, new BigDecimal("0.100"), info());
    }

    private Quartering quartering(SamplingShift shift, String sample, String white, String green, String black) {
        Quartering quartering = new Quartering(nextId++, shift.getId(), shift.calculateTotalNetWeight(),
                new BigDecimal(sample), info());
        quartering.addResult(new QuarteringResult(1L, "Papel", WasteGroup.BLANCO, new BigDecimal(white)));
        quartering.addResult(new QuarteringResult(4L, "Restos de comida", WasteGroup.VERDE, new BigDecimal(green)));
        quartering.addResult(new QuarteringResult(6L, "Sanitarios", WasteGroup.NEGRO, new BigDecimal(black)));
        shift.registerQuartering(quartering);
        return quartering;
    }

    private void study(StudyStatus status) {
        List<LocalDate> dates = new ArrayList<>();
        IntStream.range(0, 8).forEach(i -> dates.add(LocalDate.of(2026, 11, 16).plusDays(i)));
        when(studies.findById(7L)).thenReturn(Optional.of(CharacterizationStudy.restore(7L, 1L, status, dates, List.of())));
    }

    private static VerificationInfo info() {
        return new VerificationInfo(11L, NOW.minusDays(1));
    }

    private static String groupShare(StudyIndicators indicators, WasteGroup group) {
        return shown(indicators.composition().groups().stream().filter(share -> share.group() == group)
                .findFirst().orElseThrow().percentage(), 2);
    }

    private static String shown(BigDecimal value, int scale) {
        return value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }
}
