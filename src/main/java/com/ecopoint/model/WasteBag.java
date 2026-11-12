package com.ecopoint.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Bag of waste collected from one generation source at the end of a shift
 * (RN-030 to RN-037). The net weight and the density are always calculated,
 * never typed. The label is assigned by the shift when the bag is added.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class WasteBag extends VerifiableRecord {

    private static final int WEIGHT_SCALE = 2;

    private final Long sourceId;
    private final String sourceCode;
    private BagLabel label;
    private BigDecimal grossWeight;
    private BigDecimal tare;
    private BigDecimal volume;
    private boolean atypical;

    /**
     * Creates a bag before it is added to a shift.
     *
     * @param id           identifier, or null if not saved yet
     * @param sourceId     generation source the waste comes from
     * @param sourceCode   code of that source, used in the label
     * @param grossWeight  weight of the bag with its container, in kg
     * @param tare         weight of the empty bag or container, in kg
     * @param volume       volume occupied by the waste, in m³
     * @param verification who typed the bag and when
     */
    public WasteBag(Long id, Long sourceId, String sourceCode, BigDecimal grossWeight, BigDecimal tare,
                    BigDecimal volume, VerificationInfo verification) {
        super(id, verification);
        this.sourceId = sourceId;
        this.sourceCode = sourceCode;
        this.grossWeight = grossWeight;
        this.tare = tare;
        this.volume = volume;
    }

    /**
     * Calculates the net weight: gross weight minus tare (RN-034).
     *
     * @return net weight in kg with 2 decimals, or null if a weight is missing
     */
    public BigDecimal getNetWeight() {
        if (grossWeight == null || tare == null) {
            return null;
        }
        return grossWeight.subtract(tare).setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the density: net weight divided by volume (RN-036).
     *
     * @return density in kg/m³ with 2 decimals, or null if it cannot be calculated
     */
    public BigDecimal getDensity() {
        BigDecimal net = getNetWeight();
        if (net == null || volume == null || volume.signum() <= 0) {
            return null;
        }
        return net.divide(volume, WEIGHT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Returns the label written on the bag, e.g. {@code AU-D3-M}.
     *
     * @return label text, or null if the bag was not added to a shift yet
     */
    public String getLabelText() {
        return label == null ? null : label.toString();
    }

    /**
     * Assigns the label; only the shift does it, when the bag is added or when
     * a second bag of the same source forces a sequence number (RN-031).
     *
     * @param newLabel label assigned by the shift
     */
    void assignLabel(BagLabel newLabel) {
        this.label = newLabel;
    }

    @Override
    public String getRecordType() {
        return "Bolsa";
    }

    public Long getSourceId() {
        return sourceId;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public BagLabel getLabel() {
        return label;
    }

    public BigDecimal getGrossWeight() {
        return grossWeight;
    }

    public void setGrossWeight(BigDecimal grossWeight) {
        this.grossWeight = grossWeight;
    }

    public BigDecimal getTare() {
        return tare;
    }

    public void setTare(BigDecimal tare) {
        this.tare = tare;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public boolean isAtypical() {
        return atypical;
    }

    /**
     * Marks or unmarks the bag as atypical (RN-037). An atypical bag is kept,
     * but it raises a data-quality alert.
     *
     * @param atypical true if the bag is outside the plausibility limits
     */
    public void setAtypical(boolean atypical) {
        this.atypical = atypical;
    }
}
