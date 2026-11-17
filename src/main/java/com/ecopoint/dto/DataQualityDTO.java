package com.ecopoint.dto;

import com.ecopoint.service.StudyIndicators;

import java.math.BigDecimal;

/**
 * Data quality of the study (R-526).
 *
 * @param totalRecords             verifiable records
 * @param verifiedRecords          records verified or corrected
 * @param verifiedPercentage       verified share in percent
 * @param correctedRecords         records corrected against the paper form
 * @param nonConformingQuarterings non-conforming quarterings
 * @author craulgomez
 * @version 1.0
 */
public record DataQualityDTO(int totalRecords, int verifiedRecords, BigDecimal verifiedPercentage,
                             int correctedRecords, int nonConformingQuarterings) {

    /**
     * Rounds the values calculated by the indicator service (R-511).
     *
     * @param value value with full precision
     * @return the DTO
     */
    public static DataQualityDTO from(StudyIndicators.DataQuality value) {
        return new DataQualityDTO(value.totalRecords(), value.verifiedRecords(), Decimals.show(value.verifiedPercentage()),
                value.correctedRecords(), value.nonConformingQuarterings());
    }
}
