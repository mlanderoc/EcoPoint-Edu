package com.ecopoint.model;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
    private Long crewId;
    private ShiftPopulation population;
    private final List<WasteBag> bags;
    private Quartering quartering;
    private DensityMeasurement density;
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
        this.bags = new ArrayList<>();
        this.sourcesWithoutGeneration = new HashSet<>();
    }

    /**
     * Rebuilds a shift read from the database with everything recorded in it
     * (spec 002, plan R1). The bags keep their stored labels. Only the
     * persistence layer must use it.
     *
     * @param id                       shift id
     * @param studyId                  study id
     * @param dayNumber                sampling day
     * @param type                     morning or afternoon
     * @param status                   stored status
     * @param population               stored population, or null
     * @param bags                     stored bags
     * @param quartering               stored quartering, or null
     * @param density                  stored density measurement, or null
     * @param sourcesWithoutGeneration sources marked as having no generation
     * @return the rebuilt shift
     */
    public static SamplingShift restore(Long id, Long studyId, int dayNumber, ShiftType type, ShiftStatus status,
                                        ShiftPopulation population, List<WasteBag> bags, Quartering quartering,
                                        DensityMeasurement density, Set<Long> sourcesWithoutGeneration) {
        SamplingShift shift = new SamplingShift(id, studyId, dayNumber, type);
        shift.status = status;
        shift.population = population;
        shift.bags.addAll(bags);
        shift.quartering = quartering;
        shift.density = density;
        shift.sourcesWithoutGeneration.addAll(sourcesWithoutGeneration);
        return shift;
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
        if (hasBagsFrom(sourceId)) {
            throw new BusinessRuleException("La fuente ya tiene bolsas en el turno " + getCode()
                    + ": no se puede marcar como sin generación.");
        }
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
        return sourcesWithoutGeneration.contains(sourceId) || hasBagsFrom(sourceId);
    }

    /**
     * Adds a bag to the shift and assigns its label {@code SOURCE-D<day>-<M|T>}
     * (RN-031). The first bag of a source keeps the label without sequence,
     * because it is already written on the physical bag and on the paper form;
     * the following bags get {@code -2}, {@code -3}... The bag data and the population are checked beforehand by
     * {@code WasteBagValidator} and {@code ShiftValidator}.
     *
     * @param bag bag to add
     * @return the label assigned to the new bag
     * @throws BusinessRuleException if the shift is closed or the source is marked without generation
     */
    public BagLabel addBag(WasteBag bag) {
        requireOpen();
        if (sourcesWithoutGeneration.contains(bag.getSourceId())) {
            throw new BusinessRuleException("La fuente " + bag.getSourceCode()
                    + " está marcada sin generación en el turno " + getCode() + ": quite la marca primero.");
        }
        List<WasteBag> sameSource = getBagsFrom(bag.getSourceId());
        BagLabel base = BagLabel.of(bag.getSourceCode(), dayNumber, type);
        bag.assignLabel(sameSource.isEmpty() ? base : base.withSequence(sameSource.size() + 1));
        bags.add(bag);
        return bag.getLabel();
    }

    /**
     * Returns the bags of one source in this shift, in the order they were added.
     *
     * @param sourceId identifier of the source
     * @return list of bags (empty if none)
     */
    public List<WasteBag> getBagsFrom(Long sourceId) {
        return bags.stream().filter(bag -> Objects.equals(bag.getSourceId(), sourceId)).toList();
    }

    /**
     * Indicates whether a source already has bags in this shift.
     *
     * @param sourceId identifier of the source
     * @return true if at least one bag comes from that source
     */
    public boolean hasBagsFrom(Long sourceId) {
        return bags.stream().anyMatch(bag -> Objects.equals(bag.getSourceId(), sourceId));
    }

    /**
     * Indicates whether the shift already has its quartering (RN-050).
     *
     * @return true if the quartering was registered
     */
    public boolean hasQuartering() {
        return quartering != null;
    }

    /**
     * Registers the quartering of the shift; a shift has at most one (RN-050).
     * The sample data are checked beforehand by {@code QuarteringValidator}.
     *
     * @param newQuartering quartering of the shift
     * @throws BusinessRuleException if the shift is closed or already has a quartering
     */
    public void registerQuartering(Quartering newQuartering) {
        requireOpen();
        if (hasQuartering()) {
            throw new BusinessRuleException("El turno " + getCode() + " ya tiene un cuarteo registrado.");
        }
        this.quartering = newQuartering;
    }

    /**
     * Indicates whether the shift already has its density measurement (RN-057).
     *
     * @return true if the measurement was registered
     */
    public boolean hasDensity() {
        return density != null;
    }

    /**
     * Registers the density measurement of the shift; a shift has at most one
     * (RN-057). The values are checked beforehand by {@code DensityValidator}.
     *
     * @param measurement density measurement of the shift
     * @throws BusinessRuleException if the shift is closed or already has a measurement
     */
    public void registerDensity(DensityMeasurement measurement) {
        requireOpen();
        if (hasDensity()) {
            throw new BusinessRuleException("El turno " + getCode() + " ya tiene una medición de densidad.");
        }
        this.density = measurement;
    }

    /**
     * Calculates the total net weight of the shift: the sum of the net
     * weights of all its bags. It is the base of the quartering (RN-051).
     *
     * @return total net weight in kg with 2 decimals
     */
    public BigDecimal calculateTotalNetWeight() {
        return bags.stream()
                .map(WasteBag::getNetWeight)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
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

    /**
     * Returns the crew that works the shift.
     *
     * @return crew id, or null when no crew was assigned
     */
    public Long getCrewId() {
        return crewId;
    }

    /**
     * Assigns the crew that works the shift (spec 004, R-331 and R-341).
     *
     * @param crewId crew id, or null to leave the shift without crew
     */
    public void assignCrew(Long crewId) {
        this.crewId = crewId;
    }

    public ShiftStatus getStatus() {
        return status;
    }

    public ShiftPopulation getPopulation() {
        return population;
    }

    public Quartering getQuartering() {
        return quartering;
    }

    public DensityMeasurement getDensity() {
        return density;
    }

    /**
     * Returns all the bags of the shift.
     *
     * @return unmodifiable list of bags
     */
    public List<WasteBag> getBags() {
        return Collections.unmodifiableList(bags);
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
