package com.ecopoint.dto;

import com.ecopoint.model.ShiftStatus;
import com.ecopoint.model.ShiftType;
import com.ecopoint.service.ShiftSlot;

/**
 * One of the 16 possible shifts of a study, opened or not (spec 003, section 5.2).
 *
 * @param dayNumber      sampling day, 1 to 8
 * @param type           morning or afternoon
 * @param shiftId        shift id, or null if it was not opened yet
 * @param status         ABIERTO, CERRADO, or null if it was not opened yet
 * @param coveredSources participating sources already with bags or "no generation"
 * @param totalSources   participating sources of the study
 * @author craulgomez
 * @version 1.0
 */
public record ShiftSlotResponse(int dayNumber, ShiftType type, Long shiftId, ShiftStatus status, int coveredSources,
                                int totalSources) {

    /**
     * Copies a slot calculated by the shift service.
     *
     * @param slot slot of the study
     * @return the response
     */
    public static ShiftSlotResponse from(ShiftSlot slot) {
        return new ShiftSlotResponse(slot.dayNumber(), slot.type(), slot.shift() == null ? null : slot.shift().getId(),
                slot.shift() == null ? null : slot.shift().getStatus(), slot.coveredSources(), slot.totalSources());
    }
}
