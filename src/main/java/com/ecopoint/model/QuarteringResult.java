package com.ecopoint.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Weight of one waste subcategory inside the quartering sample, and its share
 * of the classified total (RN-053, RN-055).
 *
 * @author EstefaniaMarquez
 * @version 2.0
 */
public class QuarteringResult {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final Long categoryId;
    private final String categoryName;
    private final WasteGroup group;
    private BigDecimal weight;
    private BigDecimal percentage;

    /**
     * Creates a result for a subcategory.
     *
     * @param categoryId   subcategory identifier
     * @param categoryName subcategory name, e.g. "Cartón"
     * @param group        waste group of the subcategory
     * @param weight       weight of the subcategory in the sample, in kg
     */
    public QuarteringResult(Long categoryId, String categoryName, WasteGroup group, BigDecimal weight) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.group = group;
        this.weight = weight;
    }

    /**
     * Creates a result from a waste subcategory of the catalog.
     *
     * @param category subcategory being weighed
     * @param weight   weight in kg
     * @return the result
     */
    public static QuarteringResult of(WasteCategory category, BigDecimal weight) {
        return new QuarteringResult(category.getId(), category.getName(), category.getGroup(), weight);
    }

    /**
     * Calculates and stores the share of this subcategory over a total, as a
     * percentage with 2 decimals (GRASP Information Expert).
     *
     * @param total total weight the percentage refers to, in kg
     * @return the calculated percentage, or zero if the total is zero
     */
    public BigDecimal calculatePercentageOver(BigDecimal total) {
        if (weight == null || total == null || total.signum() == 0) {
            this.percentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            this.percentage = weight.multiply(ONE_HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
        }
        return percentage;
    }

    /**
     * Indicates whether this result holds hazardous waste (Sin color group)
     * with a weight greater than zero (RN-056).
     *
     * @return true for a weighed hazardous subcategory
     */
    public boolean isHazardousWithWeight() {
        return group != null && group.isHazardous() && weight != null && weight.signum() > 0;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public WasteGroup getGroup() {
        return group;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }
}
