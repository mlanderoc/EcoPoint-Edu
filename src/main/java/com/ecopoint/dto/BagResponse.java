package com.ecopoint.dto;

import com.ecopoint.model.WasteBag;

import java.math.BigDecimal;

/**
 * Bag as the API returns it, with its label and calculated values (R-343, R-344).
 *
 * @param id           bag id
 * @param shiftId      shift of the bag
 * @param sourceId     source of the waste
 * @param sourceCode   code of the source
 * @param label        label, e.g. {@code AU-D3-M}
 * @param grossWeight  gross weight, in kg
 * @param tare         tare, in kg
 * @param netWeight    net weight, in kg (2 decimals)
 * @param volume       volume, in m3
 * @param density      density, in kg/m3 (2 decimals)
 * @param atypical     whether it is outside the plausibility limits (RN-037)
 * @param verification double-entry data
 * @author craulgomez
 * @version 1.0
 */
public record BagResponse(Long id, Long shiftId, Long sourceId, String sourceCode, String label,
                          BigDecimal grossWeight, BigDecimal tare, BigDecimal netWeight, BigDecimal volume,
                          BigDecimal density, boolean atypical, VerificationDto verification) {

    /**
     * Copies a bag of the domain.
     *
     * @param shiftId shift of the bag
     * @param bag     domain bag
     * @return the response
     */
    public static BagResponse from(Long shiftId, WasteBag bag) {
        return new BagResponse(bag.getId(), shiftId, bag.getSourceId(), bag.getSourceCode(), bag.getLabelText(),
                bag.getGrossWeight(), bag.getTare(), Decimals.show(bag.getNetWeight()), bag.getVolume(),
                Decimals.show(bag.getDensity()), bag.isAtypical(), VerificationDto.from(bag.getVerification()));
    }
}
