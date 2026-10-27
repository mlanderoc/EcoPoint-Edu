package com.ecopoint.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a field inspection visit to a specific {@link EcologicalPoint}
 * conducted by a {@link User} during an {@link InspectionDay}.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class InspectionRecord {

    private int idRecord;
    private int idDay;
    private int idPoint;
    private int idUser;
    private LocalDate captureDate;
    private String pointStatus;
    private String observations;
    private List<ContainerDetail> containerDetails;
    private List<Evidence> evidences;
    private Quartering quartering;

    /**
     * Default constructor initializing child collections.
     */
    public InspectionRecord() {
        this.containerDetails = new ArrayList<>();
        this.evidences = new ArrayList<>();
    }

    /**
     * Constructs an InspectionRecord with its main attributes.
     *
     * @param idRecord     unique identifier of the inspection record
     * @param idDay        identifier of the inspection day
     * @param idPoint      identifier of the inspected ecological point
     * @param idUser       identifier of the user who conducted the inspection
     * @param captureDate  date on which the inspection was captured
     * @param pointStatus  overall status of the ecological point during the visit
     * @param observations general field observations
     */
    public InspectionRecord(int idRecord, int idDay, int idPoint, int idUser,
                            LocalDate captureDate, String pointStatus, String observations) {
        this.idRecord = idRecord;
        this.idDay = idDay;
        this.idPoint = idPoint;
        this.idUser = idUser;
        this.captureDate = captureDate;
        this.pointStatus = pointStatus;
        this.observations = observations;
        this.containerDetails = new ArrayList<>();
        this.evidences = new ArrayList<>();
    }

    /**
     * Adds a container inspection detail to this record (GRASP Creator).
     *
     * @param detail container detail to add
     */
    public void addContainerDetail(ContainerDetail detail) {
        if (detail != null) {
            detail.setIdRecord(this.idRecord);
            detail.setIdPoint(this.idPoint);
            this.containerDetails.add(detail);
        }
    }

    /**
     * Adds a photographic evidence item to this record (GRASP Creator).
     *
     * @param evidence evidence item to add
     */
    public void addEvidence(Evidence evidence) {
        if (evidence != null) {
            evidence.setIdRecord(this.idRecord);
            this.evidences.add(evidence);
        }
    }

    /**
     * Checks whether any container in this inspection reported a critical condition
     * (overflow or improper waste separation) (GRASP Information Expert).
     *
     * @return {@code true} if at least one container requires an alert
     */
    public boolean hasCriticalIncidents() {
        if (containerDetails == null) {
            return false;
        }
        for (ContainerDetail detail : containerDetails) {
            if (detail != null && (detail.requiresOverflowAlert() || detail.requiresImproperSeparationAlert())) {
                return true;
            }
        }
        return false;
    }

    public int getIdRecord() {
        return idRecord;
    }

    public void setIdRecord(int idRecord) {
        this.idRecord = idRecord;
    }

    public int getIdDay() {
        return idDay;
    }

    public void setIdDay(int idDay) {
        this.idDay = idDay;
    }

    public int getIdPoint() {
        return idPoint;
    }

    public void setIdPoint(int idPoint) {
        this.idPoint = idPoint;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public LocalDate getCaptureDate() {
        return captureDate;
    }

    public void setCaptureDate(LocalDate captureDate) {
        this.captureDate = captureDate;
    }

    public String getPointStatus() {
        return pointStatus;
    }

    public void setPointStatus(String pointStatus) {
        this.pointStatus = pointStatus;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public List<ContainerDetail> getContainerDetails() {
        return containerDetails;
    }

    public void setContainerDetails(List<ContainerDetail> containerDetails) {
        this.containerDetails = (containerDetails != null) ? containerDetails : new ArrayList<>();
    }

    public List<Evidence> getEvidences() {
        return evidences;
    }

    public void setEvidences(List<Evidence> evidences) {
        this.evidences = (evidences != null) ? evidences : new ArrayList<>();
    }

    public Quartering getQuartering() {
        return quartering;
    }

    public void setQuartering(Quartering quartering) {
        this.quartering = quartering;
    }
}
