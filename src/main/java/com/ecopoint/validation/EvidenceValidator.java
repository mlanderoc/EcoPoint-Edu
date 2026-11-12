package com.ecopoint.validation;

import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.Evidence;

import java.util.Locale;
import java.util.Map;

/**
 * Stateless validator for evidence files (RN-090): only JPG or PNG photos of
 * up to 5 MB, attached to an existing kind of record. Applies GRASP Pure
 * Fabrication and SOLID SRP.
 *
 * @author craulgomez
 * @version 1.0
 */
public final class EvidenceValidator {

    /** Maximum size of an evidence file: 5 MB. */
    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private static final Map<String, String> EXTENSION_BY_MIME_TYPE = Map.of(
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png", "image/png");

    private EvidenceValidator() {
    }

    /**
     * Validates the metadata of an evidence file.
     *
     * @param evidence evidence to validate
     * @throws InvalidDataException if the record reference is missing, the format is not
     *                              JPG or PNG, the file is empty or larger than 5 MB
     */
    public static void validate(Evidence evidence) {
        if (evidence == null) {
            throw new InvalidDataException("evidencia", "La evidencia no puede ser nula.");
        }
        if (evidence.getTarget() == null || evidence.getTargetId() == null) {
            throw new InvalidDataException("registro", "La evidencia debe pertenecer a un turno, una bolsa o un cuarteo.");
        }
        if (evidence.getFilePath() == null || evidence.getFilePath().isBlank()) {
            throw new InvalidDataException("archivo", "El archivo de la evidencia es obligatorio.");
        }
        String extension = extensionOf(evidence.getFilePath());
        String expectedMimeType = EXTENSION_BY_MIME_TYPE.get(extension);
        if (expectedMimeType == null || !expectedMimeType.equalsIgnoreCase(evidence.getMimeType())) {
            throw new InvalidDataException("formato", "Solo se aceptan fotos en formato JPG o PNG.");
        }
        if (evidence.getSizeBytes() <= 0) {
            throw new InvalidDataException("tamano", "El archivo está vacío.");
        }
        if (evidence.getSizeBytes() > MAX_SIZE_BYTES) {
            throw new InvalidDataException("tamano", "La foto no puede superar 5 MB.");
        }
        if (evidence.getDescription() != null && evidence.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidDataException("descripcion",
                    "La descripción no puede superar " + MAX_DESCRIPTION_LENGTH + " caracteres.");
        }
    }

    private static String extensionOf(String filePath) {
        int dot = filePath.lastIndexOf('.');
        return dot < 0 ? "" : filePath.substring(dot).toLowerCase(Locale.ROOT);
    }
}
