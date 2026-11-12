package com.ecopoint.dto;

import com.ecopoint.model.QualityStatus;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.QuarteringResult;
import com.ecopoint.model.WasteGroup;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Quartering as the API returns it, with its 2 % quality control and its
 * composition (RN-052 to RN-054).
 *
 * @param id                    quartering id
 * @param shiftId               shift of the sample
 * @param shiftNetWeight        kg net of the shift when the sample was taken
 * @param sampleWeight          kg of the subsample
 * @param totalClassifiedWeight sum of the subcategory weights
 * @param differenceKg          difference between the subsample and the classified total
 * @param differencePercent     that difference over the subsample, in %
 * @param qualityStatus         CONFORME, NO_CONFORME or ANULADO
 * @param results               weight and percentage of each subcategory
 * @param groupComposition      percentage of each waste group
 * @param annulReason           reason of the annulment, or null
 * @param annulledBy            user that annulled it, or null
 * @param annulledAt            when it was annulled, or null
 * @param verification          double-entry data
 * @author craulgomez
 * @version 1.0
 */
public record QuarteringResponse(Long id, Long shiftId, BigDecimal shiftNetWeight, BigDecimal sampleWeight,
                                 BigDecimal totalClassifiedWeight, BigDecimal differenceKg,
                                 BigDecimal differencePercent, QualityStatus qualityStatus, List<Result> results,
                                 Map<WasteGroup, BigDecimal> groupComposition, String annulReason, Long annulledBy,
                                 LocalDateTime annulledAt, VerificationDto verification) {

    /**
     * Weight of one subcategory in the quartering.
     *
     * @param categoryId   subcategory id
     * @param categoryName subcategory name
     * @param group        waste group
     * @param weight       weight, in kg
     * @param percentage   percentage over the classified total
     */
    public record Result(Long categoryId, String categoryName, WasteGroup group, BigDecimal weight,
                         BigDecimal percentage) {
    }

    /**
     * Copies a quartering of the domain.
     *
     * @param quartering domain quartering
     * @return the response
     */
    public static QuarteringResponse from(Quartering quartering) {
        quartering.calculateCompositionPercentages();
        List<Result> results = quartering.getResults().stream().map(QuarteringResponse::resultOf).toList();
        return new QuarteringResponse(quartering.getId(), quartering.getShiftId(),
                Decimals.show(quartering.getShiftNetWeight()), quartering.getSampleWeight(),
                Decimals.show(quartering.calculateTotalClassifiedWeight()),
                Decimals.show(quartering.calculateDifferenceKg()),
                Decimals.show(quartering.calculateDifferencePercent()), quartering.getQualityStatus(), results,
                quartering.calculateGroupComposition(), quartering.getAnnulReason(), quartering.getAnnulledBy(),
                quartering.getAnnulledAt(), VerificationDto.from(quartering.getVerification()));
    }

    private static Result resultOf(QuarteringResult result) {
        return new Result(result.getCategoryId(), result.getCategoryName(), result.getGroup(), result.getWeight(),
                Decimals.show(result.getPercentage()));
    }
}
