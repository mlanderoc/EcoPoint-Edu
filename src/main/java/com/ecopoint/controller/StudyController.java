package com.ecopoint.controller;

import com.ecopoint.dto.OpenShiftRequest;
import com.ecopoint.dto.ShiftResponse;
import com.ecopoint.dto.ShiftSlotResponse;
import com.ecopoint.dto.SourceIdsRequest;
import com.ecopoint.dto.StatusChangeRequest;
import com.ecopoint.dto.StudyDatesRequest;
import com.ecopoint.dto.StudyResponse;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.User;
import com.ecopoint.security.EcoPointUserDetails;
import com.ecopoint.service.ShiftService;
import com.ecopoint.service.StudyService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Studies, their days, sources and status, and their shifts (spec 003, section 5.2).
 *
 * @author craulgomez
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/studies")
public class StudyController implements FieldNameMapping {

    private static final Map<String, String> FIELDS = Map.of(
            "fechas", "days", "fuentes", "sourceIds", "motivo", "reason", "estado", "status",
            "institucion", "institutionId", "dia", "dayNumber", "turno", "type");

    private final StudyService studies;
    private final ShiftService shifts;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param studies     study service
     * @param shifts      shift service
     * @param currentUser user of the session
     */
    public StudyController(StudyService studies, ShiftService shifts, CurrentUser currentUser) {
        this.studies = studies;
        this.shifts = shifts;
        this.currentUser = currentUser;
    }

    @Override
    public Map<String, String> fieldNames() {
        return FIELDS;
    }

    /**
     * Lists the studies of an institution; by default, the one of the user.
     *
     * @param principal     user of the session
     * @param institutionId institution id, optional
     * @return studies
     */
    @GetMapping
    public List<StudyResponse> list(@AuthenticationPrincipal EcoPointUserDetails principal,
                                    @RequestParam(required = false) Long institutionId) {
        Long institution = institutionId != null ? institutionId : currentUser.of(principal).getInstitutionId();
        return studies.list(institution).stream().map(this::toResponse).toList();
    }

    /**
     * Shows one study.
     *
     * @param id study id
     * @return the study
     */
    @GetMapping("/{id}")
    public StudyResponse get(@PathVariable Long id) {
        return toResponse(studies.get(id));
    }

    /**
     * Creates a planned study with its dates (R-330).
     *
     * @param principal user of the session
     * @param request   institution and dates
     * @return the created study
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudyResponse create(@AuthenticationPrincipal EcoPointUserDetails principal,
                                @RequestBody StudyDatesRequest request) {
        User actor = currentUser.of(principal);
        Long institution = request.institutionId() != null ? request.institutionId() : actor.getInstitutionId();
        return toResponse(studies.create(actor, institution, request.toDates()));
    }

    /**
     * Changes the dates of the days (R-330).
     *
     * @param principal user of the session
     * @param id        study id
     * @param request   dates
     * @return the saved study
     */
    @PutMapping("/{id}/days")
    public StudyResponse reschedule(@AuthenticationPrincipal EcoPointUserDetails principal, @PathVariable Long id,
                                    @RequestBody StudyDatesRequest request) {
        return toResponse(studies.reschedule(currentUser.of(principal), id, request.toDates()));
    }

    /**
     * Chooses the participating sources (R-331).
     *
     * @param principal user of the session
     * @param id        study id
     * @param request   source ids
     * @return the saved study
     */
    @PutMapping("/{id}/sources")
    public StudyResponse setSources(@AuthenticationPrincipal EcoPointUserDetails principal, @PathVariable Long id,
                                    @RequestBody SourceIdsRequest request) {
        studies.setParticipatingSources(currentUser.of(principal), id, request.sourceIds());
        return toResponse(studies.get(id));
    }

    /**
     * Moves the study to another status (RN-014 to RN-017, R-332).
     *
     * @param principal user of the session
     * @param id        study id
     * @param request   status and, to reopen, the reason
     * @return the saved study
     */
    @PostMapping("/{id}/status")
    public StudyResponse changeStatus(@AuthenticationPrincipal EcoPointUserDetails principal, @PathVariable Long id,
                                      @RequestBody StatusChangeRequest request) {
        return toResponse(studies.changeStatus(currentUser.of(principal), id, request.status(), request.reason()));
    }

    /**
     * Lists the 16 possible shifts of the study with their progress.
     *
     * @param id study id
     * @return one entry per day and shift type
     */
    @GetMapping("/{id}/shifts")
    public List<ShiftSlotResponse> shifts(@PathVariable Long id) {
        return shifts.slots(id).stream().map(ShiftSlotResponse::from).toList();
    }

    /**
     * Opens a shift of the study (R-341).
     *
     * @param principal user of the session
     * @param id        study id
     * @param request   day, type and crew
     * @return the opened shift
     */
    @PostMapping("/{id}/shifts")
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftResponse openShift(@AuthenticationPrincipal EcoPointUserDetails principal, @PathVariable Long id,
                                   @RequestBody OpenShiftRequest request) {
        SamplingShift shift = shifts.open(currentUser.of(principal), id, request.dayNumber(), request.type(),
                request.crewId());
        return ShiftResponse.from(shift, shifts.progress(shift));
    }

    private StudyResponse toResponse(CharacterizationStudy study) {
        return StudyResponse.from(study, studies.participatingSources(study.getId()),
                studies.pendingRecords(study.getId()));
    }
}
