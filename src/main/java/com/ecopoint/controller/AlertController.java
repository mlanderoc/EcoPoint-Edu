package com.ecopoint.controller;

import com.ecopoint.dto.AlertResponse;
import com.ecopoint.dto.PageResponse;
import com.ecopoint.dto.ReasonRequest;
import com.ecopoint.model.AlertType;
import com.ecopoint.security.EcoPointUserDetails;
import com.ecopoint.service.AlertService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Data-quality alerts (spec 003, section 5.3).
 *
 * @author craulgomez
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController implements FieldNameMapping {

    private static final Map<String, String> FIELDS = Map.of("observacion", "reason");

    private final AlertService alerts;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param alerts      alert service
     * @param currentUser user of the session
     */
    public AlertController(AlertService alerts, CurrentUser currentUser) {
        this.alerts = alerts;
        this.currentUser = currentUser;
    }

    @Override
    public Map<String, String> fieldNames() {
        return FIELDS;
    }

    /**
     * Lists the alerts with filters, one page at a time (R-103).
     *
     * @param studyId  study whose records caused the alert, optional
     * @param type     type of alert, optional
     * @param attended attention state, optional
     * @param page     page, starting at 0
     * @param size     page size
     * @return one page of alerts, newest first
     */
    @GetMapping
    public PageResponse<AlertResponse> list(@RequestParam(required = false) Long studyId,
                                            @RequestParam(required = false) AlertType type,
                                            @RequestParam(required = false) Boolean attended,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "50") int size) {
        return PageResponse.of(alerts.list(studyId, type, attended), page, size, AlertResponse::from);
    }

    /**
     * Marks an alert as attended with a note (RN-073).
     *
     * @param principal user of the session
     * @param id        alert id
     * @param request   what was done about it, in {@code reason}
     * @return the attended alert
     */
    @PostMapping("/{id}/attend")
    public AlertResponse attend(@AuthenticationPrincipal EcoPointUserDetails principal, @PathVariable Long id,
                                @RequestBody ReasonRequest request) {
        return AlertResponse.from(alerts.attend(currentUser.of(principal), id, request.reason()));
    }
}
