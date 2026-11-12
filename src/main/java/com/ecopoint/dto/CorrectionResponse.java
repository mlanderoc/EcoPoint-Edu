package com.ecopoint.dto;

import com.ecopoint.model.Correction;

import java.time.LocalDateTime;

/**
 * One entry of the correction history of a record (RN-063).
 *
 * @param field    corrected field
 * @param oldValue value before the correction
 * @param newValue value after the correction
 * @param reason   reason written by the user
 * @param userId   user that corrected it
 * @param at       when it was corrected
 * @author craulgomez
 * @version 1.0
 */
public record CorrectionResponse(String field, String oldValue, String newValue, String reason, Long userId,
                                 LocalDateTime at) {

    /**
     * Copies a correction of the domain.
     *
     * @param correction domain correction
     * @return the response
     */
    public static CorrectionResponse from(Correction correction) {
        return new CorrectionResponse(correction.field(), correction.oldValue(), correction.newValue(),
                correction.reason(), correction.userId(), correction.at());
    }
}
