package com.ecopoint.service;

import com.ecopoint.exceptions.InvalidDataException;

import java.util.Arrays;

/**
 * Kinds of verifiable records as the API names them in its routes, with the
 * record type stored in the database (RN-060; spec 003, section 5.3).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public enum RecordKind {

    /** Population of a shift. */
    POPULATION("population", "Población del turno"),
    /** Bag. */
    BAG("bag", "Bolsa"),
    /** Quartering with its results. */
    QUARTERING("quartering", "Cuarteo"),
    /** Density measurement. */
    DENSITY("density", "Medición de densidad");

    private final String path;
    private final String recordType;

    RecordKind(String path, String recordType) {
        this.path = path;
        this.recordType = recordType;
    }

    /**
     * Finds the kind named in a route.
     *
     * @param path name in the route, e.g. {@code bag}
     * @return the kind
     * @throws InvalidDataException if the name is unknown
     */
    public static RecordKind fromPath(String path) {
        return Arrays.stream(values())
                .filter(kind -> kind.path.equalsIgnoreCase(path))
                .findFirst()
                .orElseThrow(() -> new InvalidDataException("tipoRegistro",
                        "Tipo de registro desconocido: use population, bag, quartering o density."));
    }

    /**
     * Finds the kind of a record type stored in the database.
     *
     * @param recordType stored type, e.g. "Bolsa"
     * @return the kind
     */
    public static RecordKind fromRecordType(String recordType) {
        return Arrays.stream(values())
                .filter(kind -> kind.recordType.equals(recordType))
                .findFirst()
                .orElseThrow(() -> new InvalidDataException("tipoRegistro", "Tipo de registro desconocido."));
    }

    /**
     * Returns the name used in the routes.
     *
     * @return route name
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the record type stored in the database.
     *
     * @return record type
     */
    public String getRecordType() {
        return recordType;
    }
}
