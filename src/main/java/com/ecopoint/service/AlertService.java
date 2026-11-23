package com.ecopoint.service;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.Alert;
import com.ecopoint.model.AlertType;
import com.ecopoint.model.Permission;
import com.ecopoint.model.User;
import com.ecopoint.persistence.AlertRepository;
import com.ecopoint.persistence.StudyRepository;
import com.ecopoint.validation.AccessPolicy;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Data-quality alerts: list them with filters and attend them (RN-070 to
 * RN-073; spec 003, section 5.3). A student only works with the alerts of the
 * studies of his current crew (RN-097).
 *
 * @author craulgomez
 * @version 1.1
 */
@Service
public class AlertService {

    private final AlertRepository alerts;
    private final StudyRepository studies;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param alerts  alert repository
     * @param studies study repository
     * @param clock   clock that dates the attention
     */
    public AlertService(AlertRepository alerts, StudyRepository studies, Clock clock) {
        this.alerts = alerts;
        this.studies = studies;
        this.clock = clock;
    }

    /**
     * Lists the alerts the user may see, newest first, with optional filters.
     *
     * @param actor    user that asks
     * @param studyId  study whose records caused the alert, or null for all
     * @param type     type of alert, or null for all
     * @param attended true or false to filter by attention, or null for all
     * @return alerts
     */
    public List<Alert> list(User actor, Long studyId, AlertType type, Boolean attended) {
        return source(actor, studyId).stream()
                .filter(alert -> type == null || alert.getType() == type)
                .filter(alert -> attended == null || alert.isAttended() == attended)
                .toList();
    }

    /**
     * Marks an alert as attended with a note (RN-073). An attended alert is never deleted.
     *
     * @param actor user that attends it
     * @param id    alert id
     * @param note  what was done about it
     * @return the attended alert
     */
    public Alert attend(User actor, Long id, String note) {
        AccessPolicy.require(actor, Permission.ATTEND_ALERTS);
        Alert alert = alerts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Alerta", id));
        if (!AccessPolicy.can(actor, Permission.MANAGE_STUDIES)
                && ofCrewStudies(actor).stream().noneMatch(found -> Objects.equals(found.getId(), id))) {
            throw new AccessDeniedException("Solo los integrantes de la cuadrilla del estudio pueden atender esta alerta.");
        }
        alert.attend(actor, note, LocalDateTime.now(clock));
        return alerts.save(alert);
    }

    private List<Alert> source(User actor, Long studyId) {
        if (AccessPolicy.can(actor, Permission.MANAGE_STUDIES)) {
            return studyId == null ? alerts.findAll() : alerts.findByStudy(studyId);
        }
        if (studyId == null) {
            return ofCrewStudies(actor);
        }
        return studies.findStudyIdsOfMember(actor.getId()).contains(studyId) ? alerts.findByStudy(studyId) : List.of();
    }

    private List<Alert> ofCrewStudies(User actor) {
        return studies.findStudyIdsOfMember(actor.getId()).stream()
                .flatMap(study -> alerts.findByStudy(study).stream())
                .distinct()
                .sorted(Comparator.comparing(Alert::getCreatedAt).thenComparing(Alert::getId).reversed())
                .toList();
    }
}
