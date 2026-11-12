package com.ecopoint.validation;

import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.Evidence;
import com.ecopoint.model.EvidenceTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link EvidenceValidator} (RN-090).
 *
 * @author craulgomez
 * @version 1.0
 */
class EvidenceValidatorTest {

    private static final long ONE_MB = 1024L * 1024;

    private static Evidence photo(EvidenceTarget target, String path, String mimeType, long size) {
        return new Evidence(null, target, 8L, path, mimeType, size, "Foto de la balanza", 10L,
                LocalDateTime.of(2026, 3, 4, 12, 15));
    }

    @ParameterizedTest
    @CsvSource({
            "bolsa-AU-D3-M.jpg,  image/jpeg",
            "bolsa-AU-D3-M.JPEG, image/jpeg",
            "planilla-D3.png,    image/png"
    })
    void rn090_jpgAndPngPhotosUpToFiveMegabytesAreAccepted(String path, String mimeType) {
        assertDoesNotThrow(() -> EvidenceValidator.validate(photo(EvidenceTarget.BAG, path, mimeType, 2 * ONE_MB)));
    }

    @Test
    void rn090_exactlyFiveMegabytesIsAccepted() {
        assertDoesNotThrow(() -> EvidenceValidator.validate(
                photo(EvidenceTarget.SHIFT, "turno.jpg", "image/jpeg", EvidenceValidator.MAX_SIZE_BYTES)));
    }

    @Test
    void rn090_largerThanFiveMegabytesIsRejected() {
        assertThrows(InvalidDataException.class, () -> EvidenceValidator.validate(
                photo(EvidenceTarget.BAG, "bolsa.jpg", "image/jpeg", EvidenceValidator.MAX_SIZE_BYTES + 1)));
    }

    @ParameterizedTest
    @CsvSource({
            "informe.pdf, application/pdf",
            "foto.gif,    image/gif",
            "foto,        image/jpeg",
            "foto.png,    image/jpeg"
    })
    void rn090_otherFormatsOrMismatchedTypesAreRejected(String path, String mimeType) {
        assertThrows(InvalidDataException.class,
                () -> EvidenceValidator.validate(photo(EvidenceTarget.QUARTERING, path, mimeType, ONE_MB)));
    }

    @Test
    void rn090_emptyFileOrMissingRecordIsRejected() {
        assertThrows(InvalidDataException.class,
                () -> EvidenceValidator.validate(photo(EvidenceTarget.BAG, "bolsa.jpg", "image/jpeg", 0)));
        assertThrows(InvalidDataException.class,
                () -> EvidenceValidator.validate(photo(null, "bolsa.jpg", "image/jpeg", ONE_MB)));
    }
}
