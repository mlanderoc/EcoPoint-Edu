package com.ecopoint.service;

import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftType;

/**
 * One of the 16 possible shifts of a study (8 days, morning and afternoon),
 * opened or not yet (spec 003, section 5.2).
 *
 * @param dayNumber      sampling day, 1 to 8
 * @param type           morning or afternoon
 * @param shift          the shift, or null if it was not opened yet
 * @param coveredSources participating sources already with bags or "no generation"
 * @param totalSources   participating sources of the study
 * @author EstefaniaMarquez
 * @version 1.0
 */
public record ShiftSlot(int dayNumber, ShiftType type, SamplingShift shift, int coveredSources, int totalSources) {
}
