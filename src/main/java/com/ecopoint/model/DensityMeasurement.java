package com.ecopoint.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * In-situ volumetric weight of the waste of one shift (NMX-AA-019): the net
 * weight of the waste that fills a measuring container of known volume
 * (RN-057). The density is always calculated, never typed.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class DensityMeasurement extends VerifiableRecord {

    private final Long shiftId;
    private BigDecimal containerVolume;
    private BigDecimal netWeight;

    /**
     * Creates the density measurement of a shift.
     *
     * @param id              identifier, or null if not saved yet
     * @param shiftId         shift the waste comes from
     * @param containerVolume volume of the measuring container, in m³
     * @param netWeight       net weight of the waste inside the container, in kg
     * @param verification    who typed the measurement and when
     */
    public DensityMeasurement(Long id, Long shiftId, BigDecimal containerVolume, BigDecimal netWeight,
                              VerificationInfo verification) {
        super(id, verification);
        this.shiftId = shiftId;
        this.containerVolume = containerVolume;
        this.netWeight = netWeight;
    }

    /**
     * Calculates the density of the shift: net weight divided by the container
     * volume (RN-057).
     *
     * @return density in kg/m³ with 2 decimals, or null if it cannot be calculated
     */
    public BigDecimal getDensity() {
        if (netWeight == null || containerVolume == null || containerVolume.signum() <= 0) {
            return null;
        }
        return netWeight.divide(containerVolume, 2, RoundingMode.HALF_UP);
    }

    @Override
    public String getRecordType() {
        return "Medición de densidad";
    }

    public Long getShiftId() {
        return shiftId;
    }

    public BigDecimal getContainerVolume() {
        return containerVolume;
    }

    public void setContainerVolume(BigDecimal containerVolume) {
        this.containerVolume = containerVolume;
    }

    public BigDecimal getNetWeight() {
        return netWeight;
    }

    public void setNetWeight(BigDecimal netWeight) {
        this.netWeight = netWeight;
    }
}
