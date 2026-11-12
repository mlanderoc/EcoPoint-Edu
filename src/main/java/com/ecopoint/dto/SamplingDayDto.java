package com.ecopoint.dto;

import com.ecopoint.model.SamplingDay;

import java.time.LocalDate;

/**
 * One of the 8 days of a study (spec 004, R-330 and R-333).
 *
 * @param dayNumber day number, 1 to 8
 * @param date      date of the day, or null while it is not scheduled
 * @param discarded true for day 1, which does not enter the calculations
 * @author craulgomez
 * @version 1.0
 */
public record SamplingDayDto(int dayNumber, LocalDate date, boolean discarded) {

    /**
     * Copies a day of the domain.
     *
     * @param day domain day
     * @return the DTO
     */
    public static SamplingDayDto from(SamplingDay day) {
        return new SamplingDayDto(day.getDayNumber(), day.getDate(), day.isExcluded());
    }
}
