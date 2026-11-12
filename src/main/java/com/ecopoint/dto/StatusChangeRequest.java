package com.ecopoint.dto;

import com.ecopoint.model.StudyStatus;

/**
 * Request to move a study to another status (RN-014 to RN-017, R-332).
 *
 * @param status status wanted
 * @param reason reason; required only to reopen a closed study
 * @author craulgomez
 * @version 1.0
 */
public record StatusChangeRequest(StudyStatus status, String reason) {
}
