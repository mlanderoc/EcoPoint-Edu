package com.ecopoint.model;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Morning or afternoon shift of one sampling day (RN-020, RN-021). It is the
 * central aggregate of the field work: everything recorded during a shift
 * (population, bags, quartering, density) belongs to it, so it is the
 * Information Expert to know whether the shift can receive data or be closed.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class SamplingShift {

    private Long id;
    private final Long studyId;
    private final int dayNumber;
    private final ShiftType type;
    private ShiftStatus status;
    private ShiftPopulation population;
    private final Set<Long> sourcesWithoutGeneration;

    /**
     * Opens a new shift of a study.
     *
     * @param id        identifier, or null if not saved yet
     * @param studyId   study the shift belongs to
     * @param dayNumber sampling day, 1 to 8
     * @param type      morning or afternoon
     * @throws InvalidDataException if the study, the day or the type is invalid
     */
    public SamplingShift(Long id, Long studyId, int dayNumber, ShiftType type) {
        if (studyId == null) {
            throw new InvalidDataException("estudio", "El turno debe pertenecer a un estudio.");
        }
        if (dayNumber < 1 || dayNumber > CharacterizationStudy.SAMPLING_DAYS) {
            throw new InvalidDataException("dia",
                    "El día de muestreo debe estar entre 1 y " + CharacterizationStudy.SAMPLING_DAYS + ".");
        }
        if (type == null) {
            throw new InvalidDataException("turno", "Debe indicarse si el turno es de mañana o de tarde.");
        }
        this.id = id;
        this.studyId = studyId;
        this.dayNumber = dayNumber;
        this.type = type;
        this.status = ShiftStatus.ABIERTO;
        this.sourcesWithoutGeneration = new HashSet<>();
    }

    /**
     * Returns a short code of the shift, e.g. {@code D3-M}.
     *
     * @return day and shift code
     */
    public String getCode() {
        return "D" + dayNumber + "-" + type.getCode();
    }

    /**
     * Indicates whether the shift is open and can receive field data (RN-021).
     *
     * @return true if the shift is open
     */
    public boolean isOpen() {
        return status == ShiftStatus.ABIERTO;
    }

    /**
     * Indicates whether the population of the shift was already registered.
     *
     * @return true if the population is present
     */
    public boolean hasPopulation() {
        return population != null;
    }

    /**
     * Registers the people present at the start of the shift (RN-022). The
     * values are checked beforehand by {@code ShiftValidator}; once registered,
     * the population is changed only through a correction.
     *
     * @param newPopulation population of the shift
     * @throws BusinessRuleException if the shift is closed or already has a population
     */
    public void registerPopulation(ShiftPopulation newPopulation) {
        requireOpen();
        if (hasPopulation()) {
            throw new BusinessRuleException("La población del turno " + getCode()
                    + " ya fue registrada; si hay un error, corríjala desde la verificación.");
        }
        this.population = newPopulation;
    }

    /**
     * Marks a participating source as having generated no waste in this shift,
     * so a real zero is told apart from a forgotten record (RN-024).
     *
     * @param sourceId identifier of the source
     * @throws BusinessRuleException if the shift is closed
     */
    public void markSourceWithoutGeneration(Long sourceId) {
        requireOpen();
        sourcesWithoutGeneration.add(sourceId);
    }

    /**
     * Removes the "no generation" mark of a source.
     *
     * @param sourceId identifier of the source
     * @throws BusinessRuleException if the shift is closed
     */
    public void unmarkSourceWithoutGeneration(Long sourceId) {
        requireOpen();
        sourcesWithoutGeneration.remove(sourceId);
    }

    /**
     * Indicates whether a source is already accounted for in this shift: it
     * has bags or it is marked as having no generation (RN-024).
     *
     * @param sourceId identifier of the source
     * @return true if the source does not block closing the shift
     */
    public boolean isSourceCovered(Long sourceId) {
        return sourcesWithoutGeneration.contains(sourceId);
    }

    /**
     * Closes the shift. The conditions (RN-024) are checked beforehand by
     * {@code ShiftValidator}.
     *
     * @throws BusinessRuleException if the shift is already closed
     */
    public void close() {
        requireOpen();
        this.status = ShiftStatus.CERRADO;
    }

    /**
     * Reopens a closed shift (RN-025). The study status and the user permission
     * are checked beforehand by {@code ShiftValidator} and {@code AccessPolicy}.
     *
     * @throws BusinessRuleException if the shift is already open
     */
    public void reopen() {
        if (isOpen()) {
            throw new BusinessRuleException("El turno " + getCode() + " ya está abierto.");
        }
        this.status = ShiftStatus.ABIERTO;
    }

    private void requireOpen() {
        if (!isOpen()) {
            throw new BusinessRuleException("El turno " + getCode()
                    + " está cerrado y no admite cambios (RN-021).");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudyId() {
        return studyId;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public ShiftType getType() {
        return type;
    }

    public ShiftStatus getStatus() {
        return status;
    }

    public ShiftPopulation getPopulation() {
        return population;
    }

    /**
     * Returns the sources marked as having no generation in this shift.
     *
     * @return unmodifiable set of source identifiers
     */
    public Set<Long> getSourcesWithoutGeneration() {
        return Collections.unmodifiableSet(sourcesWithoutGeneration);
    }
}
