package com.ecopoint.dto;

import com.ecopoint.model.DensityMeasurement;

import java.math.BigDecimal;

/**
 * Density measurement of a shift (RN-057). On requests only the container
 * volume and the net weight are read.
 *
 * @param id              measurement id, or null on requests
 * @param shiftId         shift, or null on requests
 * @param containerVolume volume of the measuring container, in m3
 * @param netWeight       net weight of the waste in the container, in kg
 * @param density         density, in kg/m3 (2 decimals), or null on requests
 * @param verification    double-entry data, or null on requests
 * @author craulgomez
 * @version 1.0
 */
public record DensityDto(Long id, Long shiftId, BigDecimal containerVolume, BigDecimal netWeight, BigDecimal density,
                         VerificationDto verification) {

    /**
     * Copies a measurement of the domain.
     *
     * @param measurement domain measurement
     * @return the DTO
     */
    public static DensityDto from(DensityMeasurement measurement) {
        return new DensityDto(measurement.getId(), measurement.getShiftId(), measurement.getContainerVolume(),
                measurement.getNetWeight(), Decimals.show(measurement.getDensity()),
                VerificationDto.from(measurement.getVerification()));
    }
}
