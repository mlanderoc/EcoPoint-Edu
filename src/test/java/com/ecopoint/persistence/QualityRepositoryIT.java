package com.ecopoint.persistence;

import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.model.Alert;
import com.ecopoint.model.AlertType;
import com.ecopoint.model.Correction;
import com.ecopoint.model.Evidence;
import com.ecopoint.model.EvidenceTarget;
import com.ecopoint.model.User;
import com.ecopoint.model.VerificationStatus;
import com.ecopoint.model.WasteBag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests of the data-quality repositories against PostgreSQL:
 * verification, corrections, alerts and evidence (CA-107, CA-110, RN-063, RN-073).
 * They work on the demo data of {@code 002_seed.sql}.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class QualityRepositoryIT extends PostgresIntegrationTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 4, 7, 19, 0);

    @Autowired private VerificationRepository verifications;
    @Autowired private CorrectionRepository corrections;
    @Autowired private AlertRepository alerts;
    @Autowired private EvidenceRepository evidence;
    @Autowired private WasteBagRepository bags;
    @Autowired private UserRepository users;

    private Long executingStudy;
    private User teacher;

    @BeforeEach
    void loadSeed() {
        runScript("002_seed.sql");
        executingStudy = jdbc.queryForObject("SELECT id FROM ESTUDIO WHERE estado = 'EN_EJECUCION'", Long.class);
        teacher = users.findByEmail("docente.demo@unicesar.edu.co").orElseThrow();
    }

    private WasteBag pendingBag() {
        Long id = jdbc.queryForObject("SELECT MIN(b.id) FROM BOLSA b JOIN TURNO t ON t.id = b.id_turno "
                + "WHERE t.id_estudio = ? AND b.estado_verificacion = 'PENDIENTE'", Long.class, executingStudy);
        return bags.findById(id).orElseThrow();
    }

    @Test
    void ca110_verifyingARecordRemovesItFromThePendingList() {
        List<PendingRecord> before = verifications.findPendingByStudy(executingStudy);
        WasteBag bag = pendingBag();

        bag.getVerification().markVerified(teacher.getId(), AT);
        verifications.updateVerification(bag);

        assertEquals(16, before.size());
        assertEquals(15, verifications.findPendingByStudy(executingStudy).size());
        assertEquals(VerificationStatus.VERIFICADO, bags.findById(bag.getId()).orElseThrow().getVerification().getStatus());
    }

    @Test
    void rn063_correctionIsStoredWithOldAndNewValueAndCanBeRead() {
        WasteBag bag = pendingBag();
        Correction correction = Correction.of(bag, "pesoBruto", bag.getGrossWeight(), new BigDecimal("12.04"),
                "Error de digitación", teacher.getId(), AT);

        corrections.save(correction);

        List<Correction> history = corrections.findByRecord("Bolsa", bag.getId());
        assertEquals(1, history.size());
        assertEquals(bag.getGrossWeight().toPlainString(), history.get(0).oldValue());
        assertEquals("12.04", history.get(0).newValue());
        assertEquals("Error de digitación", history.get(0).reason());
    }

    @Test
    void ca107_correctionHistoryCannotBeModifiedOrDeleted() {
        Long id = corrections.save(Correction.of(pendingBag(), "tara", "0.20", "0.30", "Revisión", teacher.getId(), AT));

        assertThrows(Exception.class, () -> jdbc.update("UPDATE CORRECCION SET motivo = 'otro' WHERE id = ?", id));
        assertThrows(Exception.class, () -> jdbc.update("DELETE FROM CORRECCION WHERE id = ?", id));
        assertEquals("Revisión", corrections.findByRecord("Bolsa", pendingBag().getId()).get(0).reason());
    }

    @Test
    void rn073_alertIsStoredAttendedAndReadBack() {
        Alert alert = alerts.save(new Alert(null, AlertType.VERIFICACION_PENDIENTE, "Turno", 1L,
                "El turno D2-M tiene registros pendientes.", AT));
        assertTrue(alerts.findPending().stream().anyMatch(a -> a.getId().equals(alert.getId())));

        alert.attend(teacher, "Se verificaron con la planilla", AT.plusHours(1));
        alerts.save(alert);

        Alert found = alerts.findById(alert.getId()).orElseThrow();
        assertTrue(found.isAttended());
        assertEquals(teacher.getId(), found.getAttendedBy());
        assertEquals(0, alerts.findPending().size());
        assertEquals(3, alerts.findAll().size());
    }

    @Test
    void ca110_evidenceMetadataIsStoredAndListedByRecord() {
        WasteBag bag = pendingBag();
        evidence.save(new Evidence(null, EvidenceTarget.BAG, bag.getId(), "evidencias/bolsa-1.jpg", "image/jpeg",
                250_000, "Foto de la balanza", teacher.getId(), AT));

        List<Evidence> found = evidence.findByTarget(EvidenceTarget.BAG, bag.getId());

        assertEquals(1, found.size());
        assertEquals("image/jpeg", found.get(0).getMimeType());
        assertTrue(evidence.findByTarget(EvidenceTarget.SHIFT, bag.getId()).isEmpty());
    }

    @Test
    void rn090_evidenceLargerThanFiveMegabytesIsRejectedByTheDatabase() {
        assertThrows(PersistenceException.class, () -> evidence.save(new Evidence(null, EvidenceTarget.BAG, 1L,
                "evidencias/grande.jpg", "image/jpeg", 6_000_000, null, teacher.getId(), AT)));
    }

    @Test
    void t17_alertsAreListedByTheStudyOfTheirRecord() {
        Long closed = jdbc.queryForObject("SELECT id FROM ESTUDIO WHERE estado = 'CERRADO'", Long.class);

        assertEquals(2, alerts.findByStudy(closed).size());
        assertTrue(alerts.findByStudy(executingStudy).isEmpty());
    }

    @Test
    void t18_evidenceIsFoundByIdAndRecordsKnowTheirShift() {
        WasteBag bag = pendingBag();
        Evidence saved = evidence.save(new Evidence(null, EvidenceTarget.BAG, bag.getId(), "evidencias/bolsa-2.png",
                "image/png", 120_000, null, teacher.getId(), AT));

        assertEquals("evidencias/bolsa-2.png", evidence.findById(saved.getId()).orElseThrow().getFilePath());
        assertTrue(evidence.findById(999_999L).isEmpty());

        Long shiftOfBag = jdbc.queryForObject("SELECT id_turno FROM BOLSA WHERE id = ?", Long.class, bag.getId());
        assertEquals(shiftOfBag, verifications.findShiftOfRecord("Bolsa", bag.getId()).orElseThrow());
        assertTrue(verifications.findShiftOfRecord("Bolsa", 999_999L).isEmpty());
        assertTrue(verifications.findShiftOfRecord("Desconocido", bag.getId()).isEmpty());
    }
}
