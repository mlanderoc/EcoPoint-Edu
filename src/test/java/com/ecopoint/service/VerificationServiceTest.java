package com.ecopoint.service;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.Correction;
import com.ecopoint.model.Institution;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftPopulation;
import com.ecopoint.model.ShiftType;
import com.ecopoint.model.StudyStatus;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.model.VerificationInfo;
import com.ecopoint.model.VerificationStatus;
import com.ecopoint.model.WasteBag;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.QuarteringResult;
import com.ecopoint.model.QualityStatus;
import com.ecopoint.model.WasteCategory;
import com.ecopoint.model.WasteGroup;
import com.ecopoint.model.Alert;
import com.ecopoint.model.AlertType;
import com.ecopoint.persistence.AlertRepository;
import com.ecopoint.persistence.CorrectionRepository;
import com.ecopoint.persistence.DensityRepository;
import com.ecopoint.persistence.InstitutionRepository;
import com.ecopoint.persistence.PendingRecord;
import com.ecopoint.persistence.QuarteringRepository;
import com.ecopoint.persistence.ShiftRepository;
import com.ecopoint.persistence.StudyRepository;
import com.ecopoint.persistence.VerificationRepository;
import com.ecopoint.persistence.WasteBagRepository;
import com.ecopoint.persistence.WasteCategoryRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Double entry: verification and correction with history (RN-060 to RN-063).
 *
 * @author EstefaniaMarquez
 */
class VerificationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 16, 0);

    private VerificationRepository verifications;
    private CorrectionRepository corrections;
    private StudyRepository studies;
    private WasteBagRepository bags;
    private ShiftRepository shifts;
    private QuarteringRepository quarterings;
    private AlertRepository alerts;
    private WasteCategoryRepository categories;
    private FieldAccess fieldAccess;
    private VerificationService service;
    private SamplingShift shift;
    private WasteBag bag;
    private final User typist = new User(11L, "Laura", "Pérez", "l@unicesar.edu.co",
            UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
    private final User checker = new User(12L, "Mario", "Gómez", "m@unicesar.edu.co",
            UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);

    @BeforeEach
    void setUp() {
        verifications = mock(VerificationRepository.class);
        corrections = mock(CorrectionRepository.class);
        studies = mock(StudyRepository.class);
        bags = mock(WasteBagRepository.class);
        shifts = mock(ShiftRepository.class);
        InstitutionRepository institutions = mock(InstitutionRepository.class);
        quarterings = mock(QuarteringRepository.class);
        alerts = mock(AlertRepository.class);
        categories = mock(WasteCategoryRepository.class);
        fieldAccess = mock(FieldAccess.class);
        service = new VerificationService(verifications, corrections, shifts, studies, bags,
                quarterings, mock(DensityRepository.class), institutions, alerts, categories,
                new QuarteringReconciler(quarterings, alerts), fieldAccess, Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

        study(StudyStatus.EN_DEPURACION);
        when(institutions.findById(1L)).thenReturn(Optional.of(new Institution(1L, "I.E.", "V")));
        shift = new SamplingShift(30L, 7L, 3, ShiftType.MANANA);
        shift.registerPopulation(new ShiftPopulation(60L, 300, 12, 6, new VerificationInfo(11L, NOW.minusDays(1))));
        bag = new WasteBag(50L, 1L, "AU", new BigDecimal("12.40"), new BigDecimal("0.40"), new BigDecimal("0.080"),
                new VerificationInfo(11L, NOW.minusDays(1)));
        shift.addBag(bag);
        when(shifts.findById(30L)).thenReturn(Optional.of(shift));
        when(verifications.findShiftOfRecord("Bolsa", 50L)).thenReturn(Optional.of(30L));
        when(verifications.findShiftOfRecord("Población del turno", 60L)).thenReturn(Optional.of(30L));
    }

    private Quartering quarteringOf(String sample, String classified) {
        Quartering quartering = new Quartering(70L, 30L, shift.calculateTotalNetWeight(), new BigDecimal(sample),
                new VerificationInfo(11L, NOW.minusDays(1)));
        quartering.addResult(new QuarteringResult(1L, "Papel", WasteGroup.BLANCO, new BigDecimal(classified)));
        quartering.evaluateQuality(new BigDecimal("2"));
        shift.registerQuartering(quartering);
        when(verifications.findShiftOfRecord("Cuarteo", 70L)).thenReturn(Optional.of(30L));
        return quartering;
    }

    @Test
    void ca087_lighterBagMakesTheQuarteringNonConformingWithAnAlert() {
        Quartering quartering = quarteringOf("12.00", "12.00");
        assertEquals(QualityStatus.CONFORME, quartering.getQualityStatus());

        service.correct(checker, RecordKind.BAG, 50L, Map.of("grossWeight", new BigDecimal("10.40")),
                "En el papel dice 10,40");

        assertEquals(QualityStatus.NO_CONFORME, quartering.getQualityStatus());
        verify(alerts).save(any());
        verify(quarterings).save(quartering);
    }

    @Test
    void ca087_alertExplainsThatTheSampleNoLongerFitsTheShift() {
        quarteringOf("12.00", "12.00");

        service.correct(checker, RecordKind.BAG, 50L, Map.of("grossWeight", new BigDecimal("10.40")),
                "En el papel dice 10,40");

        ArgumentCaptor<Alert> alert = ArgumentCaptor.forClass(Alert.class);
        verify(alerts).save(alert.capture());
        assertTrue(alert.getValue().getDescription().contains("ya no corresponde"));
        assertTrue(alert.getValue().getDescription().contains("10,00 kg"));
    }

    @Test
    void ca088_forgottenSubcategoryIsAddedWithItsHistory() {
        Quartering quartering = quarteringOf("12.00", "9.00");
        assertEquals(QualityStatus.NO_CONFORME, quartering.getQualityStatus());
        WasteCategory food = new WasteCategory(2L, "Restos de comida", WasteGroup.VERDE, "Orgánicos");
        when(categories.findAllAsMap()).thenReturn(Map.of(2L, food));

        service.correct(checker, RecordKind.QUARTERING, 70L, Map.of("result:2", new BigDecimal("3.00")),
                "Faltó pesar los restos de comida");

        assertEquals(2, quartering.getResults().size());
        assertEquals(QualityStatus.CONFORME, quartering.getQualityStatus());
        ArgumentCaptor<Correction> saved = ArgumentCaptor.forClass(Correction.class);
        verify(corrections).save(saved.capture());
        assertEquals(null, saved.getValue().oldValue());
        assertEquals("3.00", saved.getValue().newValue());
    }

    @Test
    void ca094_repeatedNonConformityKeepsOneAlertAndResolvingClosesIt() {
        Quartering quartering = quarteringOf("12.00", "9.00");
        Alert open = new Alert(5L, AlertType.CUARTEO_NO_CONFORME, "Cuarteo", 70L, "No conforme", NOW);
        when(alerts.findOpenByTarget(any(), any(), any())).thenReturn(List.of(open));

        service.correct(checker, RecordKind.QUARTERING, 70L, Map.of("result:1", new BigDecimal("9.50")), "Repesado");
        verify(alerts, never()).save(any());
        assertEquals(QualityStatus.NO_CONFORME, quartering.getQualityStatus());

        service.correct(checker, RecordKind.QUARTERING, 70L, Map.of("result:1", new BigDecimal("12.00")), "Repesado");

        assertEquals(QualityStatus.CONFORME, quartering.getQualityStatus());
        assertTrue(open.isAttended());
        verify(alerts).save(open);
    }

    @Test
    void ca088_unknownSubcategoryCannotBeAdded() {
        quarteringOf("12.00", "9.00");
        when(categories.findAllAsMap()).thenReturn(Map.of());

        assertThrows(BusinessRuleException.class, () -> service.correct(checker, RecordKind.QUARTERING, 70L,
                Map.of("result:2", new BigDecimal("3.00")), "Motivo"));
    }

    @Test
    void ca093_studentOutsideTheCrewCannotSeeVerifyNorCorrect() {
        doThrow(new AccessDeniedException("Fuera de la cuadrilla")).when(fieldAccess).require(any(), any());

        assertThrows(AccessDeniedException.class, () -> service.summary(checker, 7L));
        assertThrows(AccessDeniedException.class, () -> service.shiftRecords(checker, 30L));
        assertThrows(AccessDeniedException.class, () -> service.record(checker, RecordKind.BAG, 50L));
        assertThrows(AccessDeniedException.class, () -> service.history(checker, RecordKind.BAG, 50L));
        assertThrows(AccessDeniedException.class, () -> service.verify(checker, RecordKind.BAG, 50L));
        assertThrows(AccessDeniedException.class, () -> service.correct(checker, RecordKind.BAG, 50L,
                Map.of("grossWeight", new BigDecimal("12.04")), "Motivo"));
        verify(verifications, never()).updateVerification(any());
    }

    @Test
    void ca060_personWhoTypedTheRecordCannotVerifyIt() {
        assertThrows(BusinessRuleException.class, () -> service.verify(typist, RecordKind.BAG, 50L));
        verify(verifications, never()).updateVerification(any());
    }

    @Test
    void rn062_anotherPersonVerifiesTheRecord() {
        service.verify(checker, RecordKind.BAG, 50L);

        assertEquals(VerificationStatus.VERIFICADO, bag.getVerification().getStatus());
        assertEquals(12L, bag.getVerification().getVerifiedBy());
        verify(verifications).updateVerification(bag);
    }

    @Test
    void rn063_correctionKeepsTheOldValueAndMarksTheRecordCorrected() {
        service.correct(checker, RecordKind.BAG, 50L,
                Map.of("grossWeight", new BigDecimal("12.04"), "tare", new BigDecimal("0.40")),
                "Se digitó 12,40 y en el papel dice 12,04");

        assertEquals(VerificationStatus.CORREGIDO, bag.getVerification().getStatus());
        assertEquals(0, new BigDecimal("12.04").compareTo(bag.getGrossWeight()));
        ArgumentCaptor<Correction> correction = ArgumentCaptor.forClass(Correction.class);
        verify(corrections, times(1)).save(correction.capture());
        assertEquals("grossWeight", correction.getValue().field());
        assertEquals("12.40", correction.getValue().oldValue());
        assertEquals("12.04", correction.getValue().newValue());
        verify(bags).save(30L, bag);
    }

    @Test
    void rn063_correctionNeedsAReasonAndARealChange() {
        assertThrows(InvalidDataException.class, () -> service.correct(checker, RecordKind.BAG, 50L,
                Map.of("grossWeight", new BigDecimal("12.04")), " "));
        assertThrows(InvalidDataException.class, () -> service.correct(checker, RecordKind.BAG, 50L,
                Map.of("grossWeight", new BigDecimal("12.400")), "Revisado"));
    }

    @Test
    void ca205_correctedValuesAreValidatedAgain() {
        assertThrows(InvalidDataException.class, () -> service.correct(checker, RecordKind.BAG, 50L,
                Map.of("tare", new BigDecimal("13.00")), "La tara era otra"));
        verify(corrections, never()).save(any());
    }

    @Test
    void rn063_populationIsCorrectedWithWholeNumbers() {
        service.correct(checker, RecordKind.POPULATION, 60L, Map.of("students", new BigDecimal("310")),
                "Faltaba un salón");

        assertEquals(310, shift.getPopulation().getStudents());
        assertThrows(InvalidDataException.class, () -> service.correct(checker, RecordKind.POPULATION, 60L,
                Map.of("teachers", new BigDecimal("12.5")), "Error"));
    }

    @Test
    void rn014_closedStudyDoesNotAcceptVerifications() {
        study(StudyStatus.CERRADO);

        assertThrows(BusinessRuleException.class, () -> service.verify(checker, RecordKind.BAG, 50L));
    }

    @Test
    void ca207_unknownRecordIsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.verify(checker, RecordKind.BAG, 99L));
    }

    @Test
    void rn016_pendingRecordsAreFilteredByShift() {
        when(verifications.findPendingByStudy(7L)).thenReturn(List.of(
                new PendingRecord("Bolsa", 50L, 30L), new PendingRecord("Bolsa", 51L, 31L)));

        assertEquals(1, service.pending(checker, 7L, 30L).size());
        assertEquals(2, service.pending(checker, 7L, null).size());
    }

    @Test
    void t18_recordKindIsReadFromTheRoute() {
        assertEquals(RecordKind.DENSITY, RecordKind.fromPath("Density"));
        assertEquals(RecordKind.BAG, RecordKind.fromRecordType("Bolsa"));
        assertThrows(InvalidDataException.class, () -> RecordKind.fromPath("photo"));
    }

    @Test
    void ca410_summaryShowsWhatIsPendingAndWhetherTheStudyCanClose() {
        when(shifts.findByStudy(7L)).thenReturn(List.of(shift));

        VerificationSummary summary = service.summary(checker, 7L);
        assertEquals(2, summary.pending(), "La población y la bolsa siguen pendientes");
        assertEquals("D3-M", summary.shifts().get(0).code());
        assertEquals(false, summary.readyToClose());

        bag.getVerification().markVerified(12L, NOW);
        shift.getPopulation().getVerification().markCorrected(12L, NOW);
        VerificationSummary done = service.summary(checker, 7L);
        assertEquals(0, done.pending());
        assertEquals(1, done.verified());
        assertEquals(1, done.corrected());
        assertEquals(true, done.readyToClose());
    }

    @Test
    void r420_shiftRecordsComeWithTheirKindInOrder() {
        when(shifts.findById(30L)).thenReturn(Optional.of(shift));

        List<RecordView> records = service.shiftRecords(checker, 30L);

        assertEquals(List.of(RecordKind.POPULATION, RecordKind.BAG), records.stream().map(RecordView::kind).toList());
        assertEquals(shift, records.get(1).shift());
    }

    @Test
    void r430_recordIsFoundByKindAndIdWithItsShift() {
        RecordView view = service.record(checker, RecordKind.BAG, 50L);

        assertEquals(bag, view.record());
        assertEquals(30L, view.shift().getId());
        assertThrows(ResourceNotFoundException.class, () -> service.record(checker, RecordKind.BAG, 99L));
    }

    private void study(StudyStatus status) {
        List<LocalDate> dates = new ArrayList<>();
        IntStream.range(0, 8).forEach(i -> dates.add(LocalDate.of(2026, 11, 16).plusDays(i)));
        when(studies.findById(7L)).thenReturn(Optional.of(CharacterizationStudy.restore(7L, 1L, status, dates, List.of())));
    }
}
