package com.ecopoint.service;

import com.ecopoint.model.ShiftType;

import java.util.List;

/**
 * Progress of the data cleaning of a study: what is still pending, by day and
 * shift, and whether the study can be closed (spec 005, R-424, CA-410; RN-016).
 *
 * @param shifts                   progress of each opened shift
 * @param pending                  records still pending verification
 * @param verified                 records verified without changes
 * @param corrected                records corrected against the paper form
 * @param nonConformingQuarterings quarterings outside the 2 % tolerance not yet re-weighed or annulled
 * @param readyToClose             true when nothing is pending and no quartering is non-conforming
 * @author EstefaniaMarquez
 * @version 1.0
 */
public record VerificationSummary(List<ShiftProgress> shifts, int pending, int verified, int corrected,
                                  int nonConformingQuarterings, boolean readyToClose) {

    /**
     * Progress of one shift.
     *
     * @param shiftId                shift id
     * @param dayNumber              sampling day
     * @param type                   morning or afternoon
     * @param code                   code, e.g. {@code D3-M}
     * @param pending                records still pending
     * @param verified               records verified
     * @param corrected              records corrected
     * @param nonConformingQuartering whether its quartering is non-conforming
     */
    public record ShiftProgress(Long shiftId, int dayNumber, ShiftType type, String code, int pending, int verified,
                                int corrected, boolean nonConformingQuartering) {
    }
}
