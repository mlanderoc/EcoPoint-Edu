package com.ecopoint.model;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Quartering of one shift (NMX-AA-015): a sample of about 50 kg classified by
 * waste subcategory (RN-050 to RN-055). It is the Information Expert for the
 * classified total, the 2 % quality control and the physical composition.
 *
 * @author EstefaniaMarquez
 * @version 2.0
 */
public class Quartering extends VerifiableRecord {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int SCALE = 2;

    private final Long shiftId;
    private final BigDecimal shiftNetWeight;
    private BigDecimal sampleWeight;
    private final List<QuarteringResult> results;
    private QualityStatus qualityStatus;
    private String annulReason;
    private Long annulledBy;
    private LocalDateTime annulledAt;

    /**
     * Creates the quartering of a shift.
     *
     * @param id             identifier, or null if not saved yet
     * @param shiftId        shift the sample comes from
     * @param shiftNetWeight total net weight of the shift when the sample was taken, in kg
     * @param sampleWeight   weight of the quartered sample, in kg
     * @param verification   who typed the quartering and when
     */
    public Quartering(Long id, Long shiftId, BigDecimal shiftNetWeight, BigDecimal sampleWeight,
                      VerificationInfo verification) {
        super(id, verification);
        this.shiftId = shiftId;
        this.shiftNetWeight = shiftNetWeight;
        this.sampleWeight = sampleWeight;
        this.results = new ArrayList<>();
    }

    /**
     * Adds the weight of one subcategory to the sample.
     *
     * @param result subcategory result
     */
    public void addResult(QuarteringResult result) {
        if (result != null) {
            results.add(result);
        }
    }

    /**
     * Indicates whether the whole waste of the shift was classified because it
     * weighed less than the minimum sample (RN-051).
     *
     * @return true if the sample weight equals the net weight of the shift
     */
    public boolean isFullSample() {
        return sampleWeight != null && shiftNetWeight != null && sampleWeight.compareTo(shiftNetWeight) == 0;
    }

    /**
     * Calculates the sum of the weights of all classified subcategories.
     *
     * @return classified total in kg with 2 decimals
     */
    public BigDecimal calculateTotalClassifiedWeight() {
        return results.stream()
                .map(QuarteringResult::getWeight)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the absolute difference between the classified total and the
     * sample weight (RN-052).
     *
     * @return difference in kg with 2 decimals
     */
    public BigDecimal calculateDifferenceKg() {
        if (sampleWeight == null) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return calculateTotalClassifiedWeight().subtract(sampleWeight).abs().setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the difference between the classified total and the sample,
     * as a percentage of the sample (RN-052).
     *
     * @return difference in percent with 2 decimals
     */
    public BigDecimal calculateDifferencePercent() {
        if (sampleWeight == null || sampleWeight.signum() == 0) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return calculateDifferenceKg().multiply(ONE_HUNDRED).divide(sampleWeight, SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Applies the 2 % quality control (RN-052): the quartering is CONFORME when
     * the difference is within the tolerance, otherwise NO_CONFORME. An annulled
     * quartering keeps its status.
     *
     * @param tolerancePercent maximum allowed difference, in percent
     * @return the resulting quality status
     */
    public QualityStatus evaluateQuality(BigDecimal tolerancePercent) {
        if (qualityStatus == QualityStatus.ANULADO) {
            return qualityStatus;
        }
        BigDecimal allowedKg = sampleWeight.multiply(tolerancePercent);
        boolean withinTolerance = calculateDifferenceKg().multiply(ONE_HUNDRED).compareTo(allowedKg) <= 0;
        this.qualityStatus = withinTolerance ? QualityStatus.CONFORME : QualityStatus.NO_CONFORME;
        return qualityStatus;
    }

    /**
     * Calculates the composition of every subcategory over the classified
     * total, so the composition always adds up to 100 % (RN-053).
     */
    public void calculateCompositionPercentages() {
        BigDecimal total = calculateTotalClassifiedWeight();
        results.forEach(result -> result.calculatePercentageOver(total));
    }

    /**
     * Calculates the composition of the four waste groups over the classified
     * total (RN-054). Groups without results appear with 0.00 %.
     *
     * @return percentage of each group, with 2 decimals
     */
    public Map<WasteGroup, BigDecimal> calculateGroupComposition() {
        BigDecimal total = calculateTotalClassifiedWeight();
        Map<WasteGroup, BigDecimal> composition = new EnumMap<>(WasteGroup.class);
        for (WasteGroup group : WasteGroup.values()) {
            BigDecimal groupWeight = results.stream()
                    .filter(result -> result.getGroup() == group && result.getWeight() != null)
                    .map(QuarteringResult::getWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal percent = total.signum() == 0
                    ? BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP)
                    : groupWeight.multiply(ONE_HUNDRED).divide(total, SCALE, RoundingMode.HALF_UP);
            composition.put(group, percent);
        }
        return composition;
    }

    /**
     * Indicates whether the sample contains weighed hazardous waste, which
     * requires the hazardous-waste permission (RN-056).
     *
     * @return true if any Sin color subcategory has weight
     */
    public boolean hasHazardousResults() {
        return results.stream().anyMatch(QuarteringResult::isHazardousWithWeight);
    }

    /**
     * Annuls a non-conforming quartering with a written reason, so it no longer
     * blocks closing the study and does not count for the indicators (RN-052).
     * The permission of the user is checked by the caller.
     *
     * @param reason reason for the annulment
     * @param userId user who annuls it
     * @param at     date and time of the annulment
     * @throws BusinessRuleException if the quartering is not non-conforming
     * @throws InvalidDataException  if the reason is missing
     */
    public void annul(String reason, Long userId, LocalDateTime at) {
        if (qualityStatus != QualityStatus.NO_CONFORME) {
            throw new BusinessRuleException("Solo se puede anular un cuarteo no conforme.");
        }
        if (reason == null || reason.isBlank()) {
            throw new InvalidDataException("motivo", "Debe escribir el motivo para anular el cuarteo.");
        }
        this.qualityStatus = QualityStatus.ANULADO;
        this.annulReason = reason.trim();
        this.annulledBy = userId;
        this.annulledAt = at;
    }

    @Override
    public String getRecordType() {
        return "Cuarteo";
    }

    public Long getShiftId() {
        return shiftId;
    }

    public BigDecimal getShiftNetWeight() {
        return shiftNetWeight;
    }

    public BigDecimal getSampleWeight() {
        return sampleWeight;
    }

    public void setSampleWeight(BigDecimal sampleWeight) {
        this.sampleWeight = sampleWeight;
    }

    /**
     * Returns the subcategory results.
     *
     * @return unmodifiable list of results
     */
    public List<QuarteringResult> getResults() {
        return Collections.unmodifiableList(results);
    }

    public QualityStatus getQualityStatus() {
        return qualityStatus;
    }

    public String getAnnulReason() {
        return annulReason;
    }

    public Long getAnnulledBy() {
        return annulledBy;
    }

    public LocalDateTime getAnnulledAt() {
        return annulledAt;
    }
}
