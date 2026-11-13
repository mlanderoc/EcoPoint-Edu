package com.ecopoint.service;

import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.Correction;
import com.ecopoint.model.SamplingShift;

import java.util.List;

/**
 * Everything the Excel and PDF reports of a study need, loaded once so that
 * both use the same numbers as the dashboard (spec 006, R-533).
 *
 * @param study           study with its days
 * @param institutionName name of the institution
 * @param shifts          shifts with everything recorded in them
 * @param indicators      indicators with full precision
 * @param corrections     correction history of the study records
 * @author EstefaniaMarquez
 * @version 1.0
 */
public record StudyReport(CharacterizationStudy study, String institutionName, List<SamplingShift> shifts,
                          StudyIndicators indicators, List<Correction> corrections) {
}
