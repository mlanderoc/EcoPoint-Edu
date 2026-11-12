package com.ecopoint.model;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;

import java.time.LocalDateTime;

/**
 * Automatic data-quality alert (RN-070 to RN-073): a non-conforming
 * quartering, an atypical bag or records still pending verification. An
 * attended alert is never deleted; it keeps who attended it, when and how.
 *
 * @author EstefaniaMarquez
 * @version 2.0
 */
public class Alert {

    private Long id;
    private final AlertType type;
    private final String targetType;
    private final Long targetId;
    private final String description;
    private final LocalDateTime createdAt;
    private boolean attended;
    private Long attendedBy;
    private LocalDateTime attendedAt;
    private String attendNote;

    /**
     * Creates a pending alert. Alerts are normally built by {@link AlertFactory}.
     *
     * @param id          identifier, or null if not saved yet
     * @param type        alert type
     * @param targetType  kind of record that caused the alert, in Spanish (e.g. "Cuarteo")
     * @param targetId    identifier of that record
     * @param description explanation in Spanish
     * @param createdAt   date and time the alert was raised
     */
    public Alert(Long id, AlertType type, String targetType, Long targetId, String description,
                 LocalDateTime createdAt) {
        this.id = id;
        this.type = type;
        this.targetType = targetType;
        this.targetId = targetId;
        this.description = description;
        this.createdAt = createdAt;
    }

    /**
     * Rebuilds an alert read from the database, including its attention data
     * (spec 002, plan R1). Only the persistence layer must use it.
     *
     * @param id          alert id
     * @param type        alert type
     * @param targetType  kind of record that caused it
     * @param targetId    id of that record
     * @param description explanation in Spanish
     * @param createdAt   when it was raised
     * @param attended    whether it was attended
     * @param attendedBy  who attended it, or null
     * @param attendedAt  when it was attended, or null
     * @param attendNote  attention note, or null
     * @return the rebuilt alert
     */
    public static Alert restore(Long id, AlertType type, String targetType, Long targetId, String description,
                                LocalDateTime createdAt, boolean attended, Long attendedBy, LocalDateTime attendedAt,
                                String attendNote) {
        Alert alert = new Alert(id, type, targetType, targetId, description, createdAt);
        alert.attended = attended;
        alert.attendedBy = attendedBy;
        alert.attendedAt = attendedAt;
        alert.attendNote = attendNote;
        return alert;
    }

    /**
     * Marks the alert as attended, with a mandatory note (RN-073).
     *
     * @param user user attending the alert
     * @param note what was done about it
     * @param at   date and time of the attention
     * @throws AccessDeniedException if the user may not attend alerts
     * @throws BusinessRuleException if the alert was already attended
     * @throws InvalidDataException  if the note is missing
     */
    public void attend(User user, String note, LocalDateTime at) {
        if (user == null || !user.hasPermission(Permission.ATTEND_ALERTS)) {
            throw new AccessDeniedException("Su usuario no tiene permiso para "
                    + Permission.ATTEND_ALERTS.getActionDescription() + ".");
        }
        if (attended) {
            throw new BusinessRuleException("La alerta ya fue atendida.");
        }
        if (note == null || note.isBlank()) {
            throw new InvalidDataException("observacion",
                    "Escriba una observación sobre cómo se atendió la alerta.");
        }
        this.attended = true;
        this.attendedBy = user.getId();
        this.attendedAt = at;
        this.attendNote = note.trim();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AlertType getType() {
        return type;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isAttended() {
        return attended;
    }

    public Long getAttendedBy() {
        return attendedBy;
    }

    public LocalDateTime getAttendedAt() {
        return attendedAt;
    }

    public String getAttendNote() {
        return attendNote;
    }
}
