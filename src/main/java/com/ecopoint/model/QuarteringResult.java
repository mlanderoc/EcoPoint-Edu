package com.ecopoint.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Represents the weighing and composition result for a specific {@link WasteCategory}
 * within a {@link Quartering} procedure.
 * Computes its own percentage relative to the quartering sample weight
 * following the GRASP Information Expert pattern.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class QuarteringResult {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    private int idQuartering;
    private int idResult;
    private int idWaste;
    private BigDecimal weight;
    private int quantity;
    private BigDecimal percentage;

    /**
     * Default constructor.
     */
    public QuarteringResult() {
    }

    /**
     * Constructs a QuarteringResult with all attributes.
     *
     * @param idQuartering identifier of the parent quartering procedure
     * @param idResult     sequential result identifier
     * @param idWaste      identifier of the classified waste category
     * @param weight       measured weight in kilograms
     * @param quantity     counted item quantity
     * @param percentage   calculated composition percentage over the sample weight
     */
    public QuarteringResult(int idQuartering, int idResult, int idWaste,
                            BigDecimal weight, int quantity, BigDecimal percentage) {
        this.idQuartering = idQuartering;
        this.idResult = idResult;
        this.idWaste = idWaste;
        this.weight = weight;
        this.quantity = quantity;
        this.percentage = percentage;
    }

    /**
     * Calculates and updates the composition percentage of this category relative
     * to the given sample weight: {@code (weight * 100) / sampleWeight}, rounded
     * to 2 decimal places using {@link RoundingMode#HALF_UP} (GRASP Information Expert).
     *
     * @param sampleWeight total weight of the quartering sample
     * @return calculated percentage rounded to 2 decimal places
     */
    public BigDecimal calculatePercentageOver(BigDecimal sampleWeight) {
        if (weight == null || sampleWeight == null || sampleWeight.compareTo(BigDecimal.ZERO) <= 0) {
            this.percentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return this.percentage;
        }
        this.percentage = weight.multiply(ONE_HUNDRED)
                .divide(sampleWeight, 2, RoundingMode.HALF_UP);
        return this.percentage;
    }

    public int getIdQuartering() {
        return idQuartering;
    }

    public void setIdQuartering(int idQuartering) {
        this.idQuartering = idQuartering;
    }

    public int getIdResult() {
        return idResult;
    }

    public void setIdResult(int idResult) {
        this.idResult = idResult;
    }

    public int getIdWaste() {
        return idWaste;
    }

    public void setIdWaste(int idWaste) {
        this.idWaste = idWaste;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }
}
