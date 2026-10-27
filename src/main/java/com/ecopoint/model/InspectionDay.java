package com.ecopoint.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a scheduled environmental characterization day (Jornada)
 * during which ecological points are inspected across the institution.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class InspectionDay {

    private int idDay;
    private LocalDate date;
    private String observations;
    private List<InspectionRecord> inspectionRecords;

    /**
     * Default constructor initializing the inspection records list.
     */
    public InspectionDay() {
        this.inspectionRecords = new ArrayList<>();
    }

    /**
     * Constructs an InspectionDay with its primary attributes.
     *
     * @param idDay        unique identifier of the inspection day
     * @param date         calendar date on which the inspection day takes place
     * @param observations general notes about the characterization day
     */
    public InspectionDay(int idDay, LocalDate date, String observations) {
        this.idDay = idDay;
        this.date = date;
        this.observations = observations;
        this.inspectionRecords = new ArrayList<>();
    }

    /**
     * Associates a field inspection record with this inspection day.
     *
     * @param record inspection record to add
     */
    public void addInspectionRecord(InspectionRecord record) {
        if (record != null) {
            record.setIdDay(this.idDay);
            this.inspectionRecords.add(record);
        }
    }

    public int getIdDay() {
        return idDay;
    }

    public void setIdDay(int idDay) {
        this.idDay = idDay;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public List<InspectionRecord> getInspectionRecords() {
        return inspectionRecords;
    }

    public void setInspectionRecords(List<InspectionRecord> inspectionRecords) {
        this.inspectionRecords = (inspectionRecords != null) ? inspectionRecords : new ArrayList<>();
    }
}
