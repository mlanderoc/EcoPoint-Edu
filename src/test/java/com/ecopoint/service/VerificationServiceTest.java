package com.ecopoint.service;

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
        service = new VerificationService(verifications, corrections, shifts, studies, bags,
                mock(QuarteringRepository.class), mock(DensityRepository.class), institutions,
                mock(AlertRepository.class), Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

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

        List<RecordView> records = service.shiftRecords(30L);

        assertEquals(List.of(RecordKind.POPULATION, RecordKind.BAG), records.stream().map(RecordView::kind).toList());
        assertEquals(shift, records.get(1).shift());
    }

    @Test
    void r430_recordIsFoundByKindAndIdWithItsShift() {
        RecordView view = service.record(RecordKind.BAG, 50L);

        assertEquals(bag, view.record());
        assertEquals(30L, view.shift().getId());
        assertThrows(ResourceNotFoundException.class, () -> service.record(RecordKind.BAG, 99L));
    }

    private void study(StudyStatus status) {
        List<LocalDate> dates = new ArrayList<>();
        IntStream.range(0, 8).forEach(i -> dates.add(LocalDate.of(2026, 11, 16).plusDays(i)));
        when(studies.findById(7L)).thenReturn(Optional.of(CharacterizationStudy.restore(7L, 1L, status, dates, List.of())));
    }
}
