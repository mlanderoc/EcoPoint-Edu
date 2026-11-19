package com.ecopoint.dto;

import com.ecopoint.model.ShiftType;

/**
 * Request to open a shift. The crew is the one of the study (R-341, RN-088).
 *
 * @param dayNumber sampling day, 1 to 8
 * @param type      morning or afternoon
 * @author craulgomez
 * @version 1.0
 */
public record OpenShiftRequest(int dayNumber, ShiftType type) {
}
