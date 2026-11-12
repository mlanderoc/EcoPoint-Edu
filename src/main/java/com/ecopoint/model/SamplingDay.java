package com.ecopoint.model;

import com.ecopoint.exceptions.InvalidDataException;

import java.time.LocalDate;

/**
 * One of the 8 sampling days of a characterization study (RN-010, RN-011).
 * Day 1 is registered like the others but excluded from the baseline
 * calculations, because it collects waste accumulated before the study (RN-012).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class SamplingDay {

    /** Day that is registered but excluded from the calculations (RN-012). */
    public static final int EXCLUDED_DAY = 1;

    private final int dayNumber;
    private LocalDate date;

    /**
     * Creates a sampling day.
     *
     * @param dayNumber day number, 1 to 8
     * @param date      calendar date of the day, or null if not scheduled yet
     * @throws InvalidDataException if the day number is outside 1 to 8
     */
    public SamplingDay(int dayNumber, LocalDate date) {
        if (dayNumber < 1 || dayNumber > CharacterizationStudy.SAMPLING_DAYS) {
            throw new InvalidDataException("dia",
                    "El día de muestreo debe estar entre 1 y " + CharacterizationStudy.SAMPLING_DAYS + ".");
        }
        this.dayNumber = dayNumber;
        this.date = date;
    }

    /**
     * Indicates whether the day is excluded from the baseline calculations.
     *
     * @return true for day 1
     */
    public boolean isExcluded() {
        return dayNumber == EXCLUDED_DAY;
    }

    /**
     * Indicates whether the day already has a calendar date.
     *
     * @return true if the date is set
     */
    public boolean isScheduled() {
        return date != null;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
