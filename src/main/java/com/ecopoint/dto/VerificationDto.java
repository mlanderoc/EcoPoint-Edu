package com.ecopoint.dto;

import com.ecopoint.model.VerificationInfo;
import com.ecopoint.model.VerificationStatus;

import java.time.LocalDateTime;

/**
 * Double-entry data of a field record: who typed it and who verified it (RN-060 to RN-062).
 *
 * @param status     PENDIENTE, VERIFICADO or CORREGIDO
 * @param enteredBy  user that typed the record
 * @param enteredAt  when it was typed
 * @param verifiedBy user that verified or corrected it, or null
 * @param verifiedAt when it was verified or corrected, or null
 * @author craulgomez
 * @version 1.0
 */
public record VerificationDto(VerificationStatus status, Long enteredBy, LocalDateTime enteredAt, Long verifiedBy,
                              LocalDateTime verifiedAt) {

    /**
     * Copies the verification data of the domain.
     *
     * @param info domain verification data
     * @return the DTO
     */
    public static VerificationDto from(VerificationInfo info) {
        return new VerificationDto(info.getStatus(), info.getEnteredBy(), info.getEnteredAt(), info.getVerifiedBy(),
                info.getVerifiedAt());
    }
}
