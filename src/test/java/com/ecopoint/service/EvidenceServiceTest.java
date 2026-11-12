package com.ecopoint.service;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.Evidence;
import com.ecopoint.model.EvidenceTarget;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.model.VerificationInfo;
import com.ecopoint.model.WasteBag;
import com.ecopoint.persistence.EvidenceRepository;
import com.ecopoint.persistence.QuarteringRepository;
import com.ecopoint.persistence.ShiftRepository;
import com.ecopoint.persistence.WasteBagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Evidence photos stored in a local folder (RN-090).
 *
 * @author EstefaniaMarquez
 */
class EvidenceServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 9, 0);
    private static final byte[] PHOTO = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};

    @TempDir
    private Path folder;

    private EvidenceRepository evidences;
    private EvidenceService service;
    private final User student = new User(11L, "Laura", "Pérez", "l@unicesar.edu.co",
            UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);

    @BeforeEach
    void setUp() {
        evidences = mock(EvidenceRepository.class);
        WasteBagRepository bags = mock(WasteBagRepository.class);
        service = new EvidenceService(evidences, mock(ShiftRepository.class), bags, mock(QuarteringRepository.class),
                folder.toString(), Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        when(bags.findById(50L)).thenReturn(Optional.of(new WasteBag(50L, 1L, "AU", new BigDecimal("12.40"),
                new BigDecimal("0.40"), new BigDecimal("0.080"), new VerificationInfo(11L, NOW))));
        when(evidences.save(any())).thenAnswer(call -> {
            Evidence evidence = call.getArgument(0);
            evidence.setId(9L);
            return evidence;
        });
    }

    @Test
    void rn090_photoOfABagIsStoredOnDiskAndItsDataInTheDatabase() throws Exception {
        Evidence saved = upload("balanza.JPG", "image/jpeg", PHOTO.length);

        assertEquals(EvidenceTarget.BAG, saved.getTarget());
        assertEquals(11L, saved.getUploadedBy());
        assertTrue(saved.getFilePath().startsWith("bag/") && saved.getFilePath().endsWith(".jpg"));
        assertArrayEquals(PHOTO, Files.readAllBytes(folder.resolve(saved.getFilePath())));

        when(evidences.findById(9L)).thenReturn(Optional.of(saved));
        assertEquals(folder.resolve(saved.getFilePath()).toAbsolutePath().normalize(), service.file(9L).path());
    }

    @Test
    void rn090_onlyJpgOrPngUpToFiveMegabytesIsAccepted() {
        assertThrows(InvalidDataException.class, () -> upload("informe.pdf", "application/pdf", PHOTO.length));
        assertThrows(InvalidDataException.class, () -> upload("grande.png", "image/png", 6_000_000));
    }

    @Test
    void rn090_rejectedPhotoLeavesNoFileBehind() throws Exception {
        doThrow(new PersistenceException("guardar la evidencia", new RuntimeException())).when(evidences).save(any());

        assertThrows(PersistenceException.class, () -> upload("balanza.png", "image/png", PHOTO.length));
        try (var files = Files.walk(folder)) {
            assertFalse(files.anyMatch(Files::isRegularFile));
        }
    }

    @Test
    void ca207_photoOfAMissingBagIsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.upload(student, EvidenceTarget.BAG, 99L,
                "balanza.jpg", "image/jpeg", PHOTO.length, new ByteArrayInputStream(PHOTO), null));
    }

    @Test
    void rn081_inactiveUserCannotUploadPhotos() {
        User inactive = new User(12L, "Ana", "Ruiz", "a@unicesar.edu.co", UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
        inactive.deactivate();

        assertThrows(AccessDeniedException.class, () -> service.upload(inactive, EvidenceTarget.BAG, 50L,
                "balanza.jpg", "image/jpeg", PHOTO.length, new ByteArrayInputStream(PHOTO), null));
    }

    @Test
    void ca207_evidenceWithoutItsFileIsNotFound() {
        when(evidences.findById(8L)).thenReturn(Optional.of(new Evidence(8L, EvidenceTarget.BAG, 50L,
                "bag/borrada.jpg", "image/jpeg", 10, null, 11L, NOW)));

        assertThrows(ResourceNotFoundException.class, () -> service.file(8L));
    }

    private Evidence upload(String name, String mimeType, long size) {
        return service.upload(student, EvidenceTarget.BAG, 50L, name, mimeType, size,
                new ByteArrayInputStream(PHOTO), "Foto de la balanza");
    }
}
