package com.ecopoint.dto;

import com.ecopoint.model.Alert;
import com.ecopoint.model.AlertType;

import java.time.LocalDateTime;

/**
 * Data-quality alert as the API returns it (RN-070 to RN-073).
 *
 * @param id          alert id
 * @param type        type of alert
 * @param targetType  kind of record that caused it, e.g. "Bolsa"
 * @param targetId    id of that record
 * @param description description in Spanish
 * @param createdAt   when it was raised
 * @param attended    whether it was attended
 * @param attendedBy  user that attended it, or null
 * @param attendedAt  when it was attended, or null
 * @param note        what was done about it, or null
 * @author craulgomez
 * @version 1.0
 */
public record AlertResponse(Long id, AlertType type, String targetType, Long targetId, String description,
                            LocalDateTime createdAt, boolean attended, Long attendedBy, LocalDateTime attendedAt,
                            String note) {

    /**
     * Copies an alert of the domain.
     *
     * @param alert domain alert
     * @return the response
     */
    public static AlertResponse from(Alert alert) {
        return new AlertResponse(alert.getId(), alert.getType(), alert.getTargetType(), alert.getTargetId(),
                alert.getDescription(), alert.getCreatedAt(), alert.isAttended(), alert.getAttendedBy(),
                alert.getAttendedAt(), alert.getAttendNote());
    }
}
