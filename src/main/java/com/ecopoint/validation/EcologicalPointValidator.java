package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.Container;
import com.ecopoint.model.EcologicalPoint;
import com.ecopoint.model.Institution;
import com.ecopoint.model.Zone;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stateless validator for Institution, Zone, EcologicalPoint, and Container
 * data and business rules applying GRASP Pure Fabrication and SOLID SRP.
 *
 * @author craulgomez
 * @version 1.0
 */
public final class EcologicalPointValidator {

    private static final BigDecimal MIN_COORD = BigDecimal.ZERO;
    private static final BigDecimal MAX_COORD = new BigDecimal("100.00");

    private static final Set<String> VALID_POINT_STATUSES =
            Set.of("ACTIVO", "INACTIVO", "MANTENIMIENTO", "CRITICO");

    private static final Set<String> VALID_PHYSICAL_CONDITIONS =
            Set.of("BUENO", "REGULAR", "MALO");

    private static final Set<String> MANDATORY_COLORS =
            Set.of(Container.COLOR_WHITE, Container.COLOR_GREEN, Container.COLOR_BLACK);

    private EcologicalPointValidator() {
    }

    /**
     * Validates an Institution before registration or update.
     *
     * @param institution institution to validate
     * @throws InvalidDataException if name or address is null, blank, or purely numeric
     */
    public static void validateInstitutionData(Institution institution) {
        if (institution == null) {
            throw new InvalidDataException("institución", "La institución no puede ser nula.");
        }
        validateNonNumericText(institution.getName(), "nombre de la institución", 3);
        if (institution.getAddress() == null || institution.getAddress().trim().length() < 5) {
            throw new InvalidDataException("dirección",
                    "La dirección de la institución es obligatoria y debe tener al menos 5 caracteres.");
        }
    }

    /**
     * Validates a Zone and checks for duplicate zone names within the same institution.
     *
     * @param zone          zone to validate
     * @param existingZones existing zones to check for duplicates
     * @throws InvalidDataException  if zone attributes are invalid
     * @throws BusinessRuleException if a zone with the same name already exists in the institution
     */
    public static void validateZoneData(Zone zone, List<Zone> existingZones) {
        if (zone == null) {
            throw new InvalidDataException("zona", "La zona no puede ser nula.");
        }
        if (zone.getIdInstitution() <= 0) {
            throw new InvalidDataException("idInstitucion",
                    "El identificador de la institución debe ser mayor que cero.");
        }
        validateNonNumericText(zone.getName(), "nombre de la zona", 3);

        if (existingZones != null) {
            String normalizedName = zone.getName().trim();
            for (Zone existing : existingZones) {
                if (existing != null
                        && existing.getIdZone() != zone.getIdZone()
                        && existing.getIdInstitution() == zone.getIdInstitution()
                        && existing.getName() != null
                        && existing.getName().trim().equalsIgnoreCase(normalizedName)) {
                    throw new BusinessRuleException("Ya existe una zona llamada '" + normalizedName
                            + "' registrada en esta institución.");
                }
            }
        }
    }

    /**
     * Validates all attributes and containers of an EcologicalPoint.
     *
     * @param point ecological point to validate
     * @throws InvalidDataException  if basic fields or coordinates are invalid
     * @throws BusinessRuleException if the point does not have the 3 mandatory containers
     */
    public static void validateEcologicalPointData(EcologicalPoint point) {
        if (point == null) {
            throw new InvalidDataException("puntoEcologico", "El punto ecológico no puede ser nulo.");
        }
        if (point.getIdZone() <= 0) {
            throw new InvalidDataException("idZona",
                    "El identificador de la zona debe ser mayor que cero.");
        }
        validateNonNumericText(point.getLocation(), "ubicación del punto ecológico", 3);

        if (point.getStatus() == null
                || !VALID_POINT_STATUSES.contains(point.getStatus().trim().toUpperCase())) {
            throw new InvalidDataException("estado",
                    "El estado del punto ecológico debe ser uno de: " + VALID_POINT_STATUSES + ".");
        }
        if (point.getInitialStatus() == null
                || !VALID_PHYSICAL_CONDITIONS.contains(point.getInitialStatus().trim().toUpperCase())) {
            throw new InvalidDataException("estadoInicial",
                    "El estado inicial del punto debe ser uno de: " + VALID_PHYSICAL_CONDITIONS + ".");
        }

        validateCoordinates(point.getCoordX(), point.getCoordY());
        validateRegulatoryContainers(point.getContainers());
    }

    /**
     * Validates that relative map coordinates coordX and coordY fall within
     * the closed interval 0.00 to 100.00.
     *
     * @param coordX horizontal percentage coordinate
     * @param coordY vertical percentage coordinate
     * @throws InvalidDataException if either coordinate is null or outside 0.00 to 100.00
     */
    public static void validateCoordinates(BigDecimal coordX, BigDecimal coordY) {
        if (coordX == null) {
            throw new InvalidDataException("coordX", "La coordenada relativa X es obligatoria.");
        }
        if (coordY == null) {
            throw new InvalidDataException("coordY", "La coordenada relativa Y es obligatoria.");
        }
        if (coordX.compareTo(MIN_COORD) < 0 || coordX.compareTo(MAX_COORD) > 0) {
            throw new InvalidDataException("coordX",
                    "La coordenada X debe estar entre 0.00% y 100.00% (valor recibido: " + coordX + ").");
        }
        if (coordY.compareTo(MIN_COORD) < 0 || coordY.compareTo(MAX_COORD) > 0) {
            throw new InvalidDataException("coordY",
                    "La coordenada Y debe estar entre 0.00% y 100.00% (valor recibido: " + coordY + ").");
        }
    }

    /**
     * Validates that an ecological point has exactly the three mandatory containers
     * required by Colombian Resolution 2184 of 2019 (BLANCO, VERDE, NEGRO).
     *
     * @param containers list of containers to validate
     * @throws InvalidDataException  if container fields or capacities are invalid
     * @throws BusinessRuleException if the 3 regulatory colors are not present
     */
    public static void validateRegulatoryContainers(List<Container> containers) {
        if (containers == null || containers.size() != 3) {
            throw new BusinessRuleException(
                    "Según la Resolución 2184 de 2019, todo punto ecológico debe contar exactamente con 3 recipientes (Blanco, Verde y Negro).");
        }

        Set<String> registeredColors = new HashSet<>();
        for (Container container : containers) {
            if (container == null) {
                throw new InvalidDataException("recipiente", "Ningún recipiente del punto ecológico puede ser nulo.");
            }
            if (container.getWasteType() == null || container.getWasteType().trim().isEmpty()) {
                throw new InvalidDataException("tipoResiduo",
                        "El tipo de residuo del recipiente es obligatorio.");
            }
            if (container.getCapacity() == null || container.getCapacity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidDataException("capacidad",
                        "La capacidad del recipiente debe ser mayor que cero.");
            }
            if (container.getColor() == null || container.getColor().trim().isEmpty()) {
                throw new InvalidDataException("color", "El color del recipiente es obligatorio.");
            }

            String normalizedColor = container.getColor().trim().toUpperCase();
            if (!MANDATORY_COLORS.contains(normalizedColor)) {
                throw new BusinessRuleException("El color '" + container.getColor()
                        + "' no es válido. La Resolución 2184 de 2019 exige únicamente los colores BLANCO, VERDE y NEGRO.");
            }
            if (!registeredColors.add(normalizedColor)) {
                throw new BusinessRuleException("El color de recipiente '" + normalizedColor
                        + "' está duplicado en el mismo punto ecológico.");
            }
        }
    }

    /**
     * Verifies that an EcologicalPoint exists.
     *
     * @param point   ecological point reference
     * @param idPoint searched identifier
     * @throws ResourceNotFoundException if point is null
     */
    public static void validatePointExists(EcologicalPoint point, int idPoint) {
        if (point == null) {
            throw new ResourceNotFoundException("Punto Ecológico", idPoint);
        }
    }

    private static void validateNonNumericText(String value, String fieldName, int minLength) {
        if (value == null || value.trim().length() < minLength) {
            throw new InvalidDataException(fieldName,
                    "El campo '" + fieldName + "' es obligatorio y debe tener al menos " + minLength + " caracteres.");
        }
        if (value.trim().matches("^\\d+$")) {
            throw new InvalidDataException(fieldName,
                    "El campo '" + fieldName + "' no puede estar compuesto únicamente por números.");
        }
    }
}
