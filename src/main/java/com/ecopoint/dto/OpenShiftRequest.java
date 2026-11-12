package com.ecopoint.dto;

import com.ecopoint.model.ShiftType;

/**
 * Request to open a shift (R-341).
 *
 * @param dayNumber sampling day, 1 to 8
 * @param type      morning or afternoon
 * @param crewId    crew that works the shift, or null
 * @author craulgomez
 * @version 1.0
 */
public record OpenShiftRequest(int dayNumber, ShiftType type, Long crewId) {
}
