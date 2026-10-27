package com.ecopoint.model;

/**
 * Represents the qualitative inspection state of an individual {@link Container}
 * during a field visit ({@link InspectionRecord}).
 * Evaluates whether automatic alerts should be triggered for container overflow
 * or improper waste separation (GRASP Information Expert).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class ContainerDetail {

    public static final String FILL_LEVEL_EMPTY = "VACIO";
    public static final String FILL_LEVEL_LOW = "BAJO";
    public static final String FILL_LEVEL_MEDIUM = "MEDIO";
    public static final String FILL_LEVEL_HIGH = "ALTO";
    public static final String FILL_LEVEL_OVERFLOWED = "DESBORDADO";

    private int idContainerDetail;
    private int idRecord;
    private int idPoint;
    private int idContainer;
    private String fillLevel;
    private String containerCondition;
    private String signageCondition;
    private boolean improperSeparation;
    private String observations;

    /**
     * Default constructor.
     */
    public ContainerDetail() {
    }

    /**
     * Constructs a ContainerDetail with all attributes.
     *
     * @param idContainerDetail  unique identifier of this container inspection detail
     * @param idRecord           identifier of the parent inspection record
     * @param idPoint            identifier of the ecological point
     * @param idContainer        identifier of the inspected container
     * @param fillLevel          observed fill level (VACIO, BAJO, MEDIO, ALTO, DESBORDADO)
     * @param containerCondition physical condition of the bin (BUENO, REGULAR, MALO, AUSENTE)
     * @param signageCondition   condition of the label/signage (BUENO, REGULAR, MALO, AUSENTE)
     * @param improperSeparation {@code true} if cross-contamination or wrong waste was detected
     * @param observations       additional field notes for this container
     */
    public ContainerDetail(int idContainerDetail, int idRecord, int idPoint, int idContainer,
                           String fillLevel, String containerCondition, String signageCondition,
                           boolean improperSeparation, String observations) {
        this.idContainerDetail = idContainerDetail;
        this.idRecord = idRecord;
        this.idPoint = idPoint;
        this.idContainer = idContainer;
        this.fillLevel = fillLevel;
        this.containerCondition = containerCondition;
        this.signageCondition = signageCondition;
        this.improperSeparation = improperSeparation;
        this.observations = observations;
    }

    /**
     * Determines whether this container inspection requires an automatic overflow alert
     * (GRASP Information Expert).
     *
     * @return {@code true} if {@code fillLevel} equals {@code DESBORDADO}
     */
    public boolean requiresOverflowAlert() {
        return fillLevel != null && FILL_LEVEL_OVERFLOWED.equalsIgnoreCase(fillLevel.trim());
    }

    /**
     * Determines whether this container inspection requires an automatic improper
     * separation alert (GRASP Information Expert).
     *
     * @return {@code true} if {@code improperSeparation} is active
     */
    public boolean requiresImproperSeparationAlert() {
        return improperSeparation;
    }

    public int getIdContainerDetail() {
        return idContainerDetail;
    }

    public void setIdContainerDetail(int idContainerDetail) {
        this.idContainerDetail = idContainerDetail;
    }

    public int getIdRecord() {
        return idRecord;
    }

    public void setIdRecord(int idRecord) {
        this.idRecord = idRecord;
    }

    public int getIdPoint() {
        return idPoint;
    }

    public void setIdPoint(int idPoint) {
        this.idPoint = idPoint;
    }

    public int getIdContainer() {
        return idContainer;
    }

    public void setIdContainer(int idContainer) {
        this.idContainer = idContainer;
    }

    public String getFillLevel() {
        return fillLevel;
    }

    public void setFillLevel(String fillLevel) {
        this.fillLevel = fillLevel;
    }

    public String getContainerCondition() {
        return containerCondition;
    }

    public void setContainerCondition(String containerCondition) {
        this.containerCondition = containerCondition;
    }

    public String getSignageCondition() {
        return signageCondition;
    }

    public void setSignageCondition(String signageCondition) {
        this.signageCondition = signageCondition;
    }

    public boolean isImproperSeparation() {
        return improperSeparation;
    }

    public void setImproperSeparation(boolean improperSeparation) {
        this.improperSeparation = improperSeparation;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }
}
