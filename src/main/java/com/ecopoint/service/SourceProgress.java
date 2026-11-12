package com.ecopoint.service;

import com.ecopoint.model.GenerationSource;

import java.math.BigDecimal;

/**
 * What a participating source has recorded in a shift (spec 004, R-345).
 *
 * @param source         participating source
 * @param bagCount       number of bags of the source in the shift
 * @param totalNetWeight sum of the net weights of those bags, in kg
 * @param status         with bags, without generation or pending
 * @author EstefaniaMarquez
 * @version 1.0
 */
public record SourceProgress(GenerationSource source, int bagCount, BigDecimal totalNetWeight,
                             SourceShiftStatus status) {
}
