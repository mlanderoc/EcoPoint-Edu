package com.ecopoint.model;

import java.time.LocalDateTime;

/**
 * Immutable record of a closed study reopened for data cleaning (RN-017).
 *
 * @param userId identifier of the teacher who reopened the study
 * @param reason reason written by the teacher
 * @param at     date and time of the reopening
 * @author EstefaniaMarquez
 * @version 1.0
 */
public record StudyReopening(Long userId, String reason, LocalDateTime at) {
}
