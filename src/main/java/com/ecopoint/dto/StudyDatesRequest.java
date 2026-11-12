package com.ecopoint.dto;

import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.CharacterizationStudy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data to create a study or change its dates (spec 004, R-330).
 *
 * @param institutionId institution to characterize; only used when creating
 * @param days          dates of the days; a day left out stays without date
 * @author craulgomez
 * @version 1.0
 */
public record StudyDatesRequest(Long institutionId, List<SamplingDayDto> days) {

    /**
     * Puts the dates in the order of the 8 days.
     *
     * @return 8 dates, null where a day has no date
     * @throws InvalidDataException if a day number is outside 1 to 8 or repeated
     */
    public List<java.time.LocalDate> toDates() {
        List<java.time.LocalDate> dates = new ArrayList<>(
                Collections.nCopies(CharacterizationStudy.SAMPLING_DAYS, null));
        boolean[] seen = new boolean[CharacterizationStudy.SAMPLING_DAYS];
        for (SamplingDayDto day : days == null ? List.<SamplingDayDto>of() : days) {
            int index = day.dayNumber() - 1;
            if (index < 0 || index >= CharacterizationStudy.SAMPLING_DAYS || seen[index]) {
                throw new InvalidDataException("fechas", "Cada día debe estar entre 1 y "
                        + CharacterizationStudy.SAMPLING_DAYS + " y aparecer una sola vez.");
            }
            seen[index] = true;
            dates.set(index, day.date());
        }
        return dates;
    }
}
