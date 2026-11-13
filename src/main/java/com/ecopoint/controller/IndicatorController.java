package com.ecopoint.controller;

import com.ecopoint.dto.StudyIndicatorsDTO;
import com.ecopoint.security.EcoPointUserDetails;
import com.ecopoint.service.IndicatorService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Baseline indicators of a study (spec 006, R-520 to R-526). The permission
 * VIEW_INDICATORS is checked by the service (RN-081).
 *
 * @author craulgomez
 * @version 1.0
 */
@RestController
public class IndicatorController {

    private final IndicatorService indicators;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param indicators  indicator service
     * @param currentUser user of the session
     */
    public IndicatorController(IndicatorService indicators, CurrentUser currentUser) {
        this.indicators = indicators;
        this.currentUser = currentUser;
    }

    /**
     * Returns the indicators of a study, labelled "Preliminar" or "Oficial" (R-502).
     *
     * @param principal user of the session
     * @param id        study id
     * @return the rounded indicators
     */
    @GetMapping("/api/v1/studies/{id}/indicators")
    public StudyIndicatorsDTO getStudyIndicators(@AuthenticationPrincipal EcoPointUserDetails principal,
                                                 @PathVariable Long id) {
        return StudyIndicatorsDTO.from(indicators.calculate(currentUser.of(principal), id));
    }
}
