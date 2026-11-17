package com.ecopoint.dto;

import com.ecopoint.model.StudyStatus;
import com.ecopoint.service.StudyIndicators;

import java.util.List;

/**
 * Baseline indicators of a study as the API returns them; dashboard and reports read these values (R-533).
 *
 * @param studyId     study id
 * @param status      study status
 * @param official    whether the values are official (R-502)
 * @param label       "Oficial" or "Preliminar"
 * @param summary     main indicators
 * @param days        days 1 to 8
 * @param sources     generation by source, highest first
 * @param shifts      morning versus afternoon
 * @param composition total composition
 * @param quality     data quality
 * @author craulgomez
 * @version 1.0
 */
public record StudyIndicatorsDTO(Long studyId, StudyStatus status, boolean official, String label,
                                 SummaryKPIsDTO summary, List<DailyGenerationDTO> days,
                                 List<SourceGenerationDTO> sources, List<ShiftComparisonDTO> shifts,
                                 CompositionDTO composition, DataQualityDTO quality) {

    /**
     * Rounds the values calculated by the indicator service (R-511).
     *
     * @param value value with full precision
     * @return the DTO
     */
    public static StudyIndicatorsDTO from(StudyIndicators value) {
        return new StudyIndicatorsDTO(value.studyId(), value.status(), value.official(),
                value.official() ? "Oficial" : "Preliminar", SummaryKPIsDTO.from(value.summary()),
                value.days().stream().map(DailyGenerationDTO::from).toList(),
                value.sources().stream().map(SourceGenerationDTO::from).toList(),
                value.shiftTypes().stream().map(ShiftComparisonDTO::from).toList(),
                CompositionDTO.from(value.composition()), DataQualityDTO.from(value.quality()));
    }
}
