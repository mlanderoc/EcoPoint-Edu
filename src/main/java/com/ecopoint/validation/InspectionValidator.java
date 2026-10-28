package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.ContainerDetail;
import com.ecopoint.model.Evidence;
import com.ecopoint.model.InspectionDay;
import com.ecopoint.model.InspectionRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Stateless validator for InspectionDay, InspectionRecord, ContainerDetail,
 * and Evidence entities applying GRASP Pure Fabrication and SOLID SRP.
 *
 * @author craulgomez
 * @version 1.0
 */
public final class InspectionValidator {

    private static final Set<String> VALID_FILL_LEVELS =
            Set.of("VACIO", "BAJO", "MEDIO", "ALTO", "DESBORDADO");

    private static final Set<String> VALID_CONDITIONS =
            Set.of("BUENO", "REGULAR", "MALO", "AUSENTE");

    private static final Set<String> VALID_IMAGE_EXTENSIONS =
            Set.of(".jpg", ".jpeg", ".png", ".webp");

    private InspectionValidator() {
    }

    /**
     * Validates an InspectionDay before creation or update.
     *
     * @param day inspection day to validate
     * @throws InvalidDataException if the date is null or lies in the future
     */
    public static void validateInspectionDayData(InspectionDay day) {
        if (day == null) {
            throw new InvalidDataException("jornada", "Los datos de la jornada no pueden ser nulos.");
        }
        if (day.getDate() == null) {
            throw new InvalidDataException("fecha", "La fecha de la jornada de inspección es obligatoria.");
        }
        if (day.getDate().isAfter(LocalDate.now())) {
            throw new InvalidDataException("fecha",
                    "La fecha de la jornada (" + day.getDate() + ") no puede ser una fecha futura.");
        }
    }

    /**
     * Validates a complete field InspectionRecord including its three container
     * evaluations and any attached photographic evidence.
     *
     * @param record inspection record to validate
     * @throws InvalidDataException  if required attributes or fill levels are invalid
     * @throws BusinessRuleException if the inspection does not evaluate the 3 containers
     */
    public static void validateInspectionRecordData(InspectionRecord record) {
        if (record == null) {
            throw new InvalidDataException("registroInspeccion",
                    "El registro de inspección no puede ser nulo.");
        }
        if (record.getIdDay() <= 0) {
            throw new InvalidDataException("idJornada",
                    "El identificador de la jornada debe ser mayor que cero.");
        }
        if (record.getIdPoint() <= 0) {
            throw new InvalidDataException("idPunto",
                    "El identificador del punto ecológico debe ser mayor que cero.");
        }
        if (record.getIdUser() <= 0) {
            throw new InvalidDataException("idUsuario",
                    "El identificador del usuario inspector debe ser mayor que cero.");
        }
        if (record.getCaptureDate() == null) {
            throw new InvalidDataException("fechaCaptura",
                    "La fecha de captura de la inspección es obligatoria.");
        }
        if (record.getCaptureDate().isAfter(LocalDate.now())) {
            throw new InvalidDataException("fechaCaptura",
                    "La fecha de captura no puede ser posterior a la fecha actual.");
        }
        if (record.getPointStatus() == null || record.getPointStatus().trim().isEmpty()) {
            throw new InvalidDataException("estadoPunto",
                    "El estado general del punto ecológico durante la inspección es obligatorio.");
        }

        List<ContainerDetail> details = record.getContainerDetails();
        if (details == null || details.size() != 3) {
            throw new BusinessRuleException(
                    "Toda inspección de un punto ecológico debe evaluar exactamente sus 3 recipientes.");
        }
        for (ContainerDetail detail : details) {
            validateContainerDetailData(detail);
        }

        if (record.getEvidences() != null) {
            for (Evidence evidence : record.getEvidences()) {
                validateEvidenceData(evidence);
            }
        }
    }

    /**
     * Validates an individual ContainerDetail evaluation.
     *
     * @param detail container detail to validate
     * @throws InvalidDataException if fill level or physical conditions are outside allowed values
     */
    public static void validateContainerDetailData(ContainerDetail detail) {
        if (detail == null) {
            throw new InvalidDataException("detalleRecipiente",
                    "El detalle de evaluación del recipiente no puede ser nulo.");
        }
        if (detail.getIdContainer() <= 0) {
            throw new InvalidDataException("idRecipiente",
                    "El identificador del recipiente evaluado debe ser mayor que cero.");
        }
        if (detail.getFillLevel() == null
                || !VALID_FILL_LEVELS.contains(detail.getFillLevel().trim().toUpperCase())) {
            throw new InvalidDataException("nivelLlenado",
                    "El nivel de llenado debe ser uno de: " + VALID_FILL_LEVELS + ".");
        }
        if (detail.getContainerCondition() == null
                || !VALID_CONDITIONS.contains(detail.getContainerCondition().trim().toUpperCase())) {
            throw new InvalidDataException("estadoRecipiente",
                    "El estado físico del recipiente debe ser uno de: " + VALID_CONDITIONS + ".");
        }
        if (detail.getSignageCondition() == null
                || !VALID_CONDITIONS.contains(detail.getSignageCondition().trim().toUpperCase())) {
            throw new InvalidDataException("estadoSenalizacion",
                    "El estado de la señalización debe ser uno de: " + VALID_CONDITIONS + ".");
        }
    }

    /**
     * Validates photographic Evidence metadata.
     *
     * @param evidence evidence item to validate
     * @throws InvalidDataException if the file path is blank or does not have an image extension
     */
    public static void validateEvidenceData(Evidence evidence) {
        if (evidence == null) {
            throw new InvalidDataException("evidencia", "La evidencia fotográfica no puede ser nula.");
        }
        if (evidence.getFilePath() == null || evidence.getFilePath().trim().isEmpty()) {
            throw new InvalidDataException("rutaArchivo",
                    "La ruta del archivo de evidencia fotográfica es obligatoria.");
        }
        String lowerPath = evidence.getFilePath().trim().toLowerCase();
        boolean validExtension = false;
        for (String ext : VALID_IMAGE_EXTENSIONS) {
            if (lowerPath.endsWith(ext)) {
                validExtension = true;
                break;
            }
        }
        if (!validExtension) {
            throw new InvalidDataException("rutaArchivo",
                    "El archivo de evidencia debe tener una extensión de imagen válida (.jpg, .jpeg, .png, .webp).");
        }
    }

    /**
     * Verifies that an InspectionDay exists.
     *
     * @param day   inspection day reference
     * @param idDay searched identifier
     * @throws ResourceNotFoundException if day is null
     */
    public static void validateInspectionDayExists(InspectionDay day, int idDay) {
        if (day == null) {
            throw new ResourceNotFoundException("Jornada de Inspección", idDay);
        }
    }

    /**
     * Verifies that an InspectionRecord exists.
     *
     * @param record   inspection record reference
     * @param idRecord searched identifier
     * @throws ResourceNotFoundException if record is null
     */
    public static void validateInspectionRecordExists(InspectionRecord record, int idRecord) {
        if (record == null) {
            throw new ResourceNotFoundException("Registro de Inspección", idRecord);
        }
    }
}
