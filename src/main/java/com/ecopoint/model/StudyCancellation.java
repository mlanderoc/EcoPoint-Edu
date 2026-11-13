package com.ecopoint.model;

import java.time.LocalDateTime;

/**
 * Cancellation of a characterization study: who cancelled it, why and when
 * (RN-018).
 *
 * @param userId identifier of the teacher who cancelled the study
 * @param reason reason for the cancellation
 * @param at     date and time of the cancellation
 * @author EstefaniaMarquez
 * @version 1.0
 */
public record StudyCancellation(Long userId, String reason, LocalDateTime at) {
}
