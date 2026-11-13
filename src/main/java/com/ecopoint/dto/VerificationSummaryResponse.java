package com.ecopoint.dto;

import com.ecopoint.model.ShiftType;
import com.ecopoint.service.VerificationSummary;

import java.util.List;

/**
 * Progress of the data cleaning of a study (spec 005, R-424, CA-410).
 *
 * @param shifts                   progress of each opened shift
 * @param pending                  records still pending verification
 * @param verified                 records verified
 * @param corrected                records corrected
 * @param nonConformingQuarterings non-conforming quarterings not yet resolved
 * @param readyToClose             whether the study can be closed (RN-016)
 * @author craulgomez
 * @version 1.0
 */
public record VerificationSummaryResponse(List<ShiftProgress> shifts, int pending, int verified, int corrected,
                                          int nonConformingQuarterings, boolean readyToClose) {

    /**
     * Progress of one shift.
     *
     * @param shiftId                 shift id
     * @param dayNumber               sampling day
     * @param type                    morning or afternoon
     * @param code                    code, e.g. {@code D3-M}
     * @param pending                 records pending
     * @param verified                records verified
     * @param corrected               records corrected
     * @param nonConformingQuartering whether its quartering is non-conforming
     */
    public record ShiftProgress(Long shiftId, int dayNumber, ShiftType type, String code, int pending, int verified,
                                int corrected, boolean nonConformingQuartering) {
    }

    /**
     * Copies the summary calculated by the verification service.
     *
     * @param summary summary of the study
     * @return the response
     */
    public static VerificationSummaryResponse from(VerificationSummary summary) {
        return new VerificationSummaryResponse(summary.shifts().stream()
                .map(shift -> new ShiftProgress(shift.shiftId(), shift.dayNumber(), shift.type(), shift.code(),
                        shift.pending(), shift.verified(), shift.corrected(), shift.nonConformingQuartering()))
                .toList(), summary.pending(), summary.verified(), summary.corrected(),
                summary.nonConformingQuarterings(), summary.readyToClose());
    }
}
