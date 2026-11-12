package com.ecopoint.dto;

import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.StudyStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Study as the API returns it (spec 003, section 5.2).
 *
 * @param id                     study id
 * @param institutionId          institution of the study
 * @param status                 status (RN-014)
 * @param startDate              date of day 1, or null
 * @param endDate                date of day 8, or null
 * @param days                   the 8 days
 * @param participatingSourceIds sources taking part
 * @param pendingRecords         records still pending verification
 * @author craulgomez
 * @version 1.0
 */
public record StudyResponse(Long id, Long institutionId, StudyStatus status, LocalDate startDate, LocalDate endDate,
                            List<SamplingDayDto> days, List<Long> participatingSourceIds, int pendingRecords) {

    /**
     * Copies a study of the domain.
     *
     * @param study          domain study
     * @param sourceIds      participating sources
     * @param pendingRecords records pending verification
     * @return the response
     */
    public static StudyResponse from(CharacterizationStudy study, Set<Long> sourceIds, int pendingRecords) {
        return new StudyResponse(study.getId(), study.getInstitutionId(), study.getStatus(), study.getStartDate(),
                study.getEndDate(), study.getDays().stream().map(SamplingDayDto::from).toList(),
                sourceIds.stream().sorted().toList(), pendingRecords);
    }
}
