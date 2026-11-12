package com.ecopoint.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data of a quartering: the subsample and the weight of each subcategory
 * (RN-051, RN-055).
 *
 * @param sampleWeight weight of the subsample, in kg
 * @param results      weight of each subcategory
 * @author craulgomez
 * @version 1.0
 */
public record QuarteringRequest(BigDecimal sampleWeight, List<Result> results) {

    /**
     * Weight of one subcategory.
     *
     * @param categoryId subcategory id
     * @param weight     weight, in kg
     */
    public record Result(Long categoryId, BigDecimal weight) {
    }
}
