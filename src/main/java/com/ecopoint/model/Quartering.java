package com.ecopoint.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a solid waste characterization procedure using the Quartering Method
 * (Método del Cuarteo) associated with a field {@link InspectionRecord}.
 * Acts as the GRASP Information Expert for computing total classified weight,
 * physical handling loss (merma), and composition percentages across its
 * {@link QuarteringResult} items.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class Quartering {

    private int idQuartering;
    private int idRecord;
    private BigDecimal initialWeight;
    private BigDecimal sampleWeight;
    private String observations;
    private List<QuarteringResult> results;

    /**
     * Default constructor initializing the results list.
     */
    public Quartering() {
        this.results = new ArrayList<>();
    }

    /**
     * Constructs a Quartering instance with its main attributes.
     *
     * @param idQuartering  unique identifier of the quartering procedure
     * @param idRecord      identifier of the originating inspection record
     * @param initialWeight total initial waste weight collected before homogenization (kg)
     * @param sampleWeight  weight of the final quartered sample selected for characterization (kg)
     * @param observations  technical notes about the quartering execution
     */
    public Quartering(int idQuartering, int idRecord, BigDecimal initialWeight,
                      BigDecimal sampleWeight, String observations) {
        this.idQuartering = idQuartering;
        this.idRecord = idRecord;
        this.initialWeight = initialWeight;
        this.sampleWeight = sampleWeight;
        this.observations = observations;
        this.results = new ArrayList<>();
    }

    /**
     * Adds a classified category result to this quartering procedure.
     *
     * @param result quartering result item to add
     */
    public void addResult(QuarteringResult result) {
        if (result != null) {
            result.setIdQuartering(this.idQuartering);
            this.results.add(result);
        }
    }

    /**
     * Calculates the sum of weights of all classified waste categories
     * (GRASP Information Expert).
     *
     * @return sum of category weights rounded to 2 decimal places
     */
    public BigDecimal calculateTotalClassifiedWeight() {
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (results == null) {
            return total;
        }
        for (QuarteringResult result : results) {
            if (result != null && result.getWeight() != null) {
                total = total.add(result.getWeight());
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the physical sample loss (merma) defined as
     * {@code sampleWeight - totalClassifiedWeight} (GRASP Information Expert).
     *
     * @return non-negative sample loss in kilograms
     */
    public BigDecimal calculateSampleLoss() {
        if (sampleWeight == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return sampleWeight.subtract(calculateTotalClassifiedWeight())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Computes and assigns the composition percentage for each {@link QuarteringResult}
     * over {@code sampleWeight} (GRASP Information Expert).
     */
    public void calculateCompositionPercentages() {
        if (results == null || sampleWeight == null) {
            return;
        }
        for (QuarteringResult result : results) {
            if (result != null) {
                result.calculatePercentageOver(this.sampleWeight);
            }
        }
    }

    public int getIdQuartering() {
        return idQuartering;
    }

    public void setIdQuartering(int idQuartering) {
        this.idQuartering = idQuartering;
    }

    public int getIdRecord() {
        return idRecord;
    }

    public void setIdRecord(int idRecord) {
        this.idRecord = idRecord;
    }

    public BigDecimal getInitialWeight() {
        return initialWeight;
    }

    public void setInitialWeight(BigDecimal initialWeight) {
        this.initialWeight = initialWeight;
    }

    public BigDecimal getSampleWeight() {
        return sampleWeight;
    }

    public void setSampleWeight(BigDecimal sampleWeight) {
        this.sampleWeight = sampleWeight;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public List<QuarteringResult> getResults() {
        return results;
    }

    public void setResults(List<QuarteringResult> results) {
        this.results = (results != null) ? results : new ArrayList<>();
    }
}
