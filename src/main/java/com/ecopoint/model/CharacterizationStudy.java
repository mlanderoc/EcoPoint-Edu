package com.ecopoint.model;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Characterization study: the 8-day sampling campaign of one institution
 * that produces its waste baseline (RN-010 to RN-019). It is the Information
 * Expert for its days, its date range and its lifecycle.
 *
 * @author EstefaniaMarquez
 * @version 2.0
 */
public class CharacterizationStudy {

    /** Number of sampling days of every study (RN-010). */
    public static final int SAMPLING_DAYS = 8;

    private Long id;
    private Long institutionId;
    private StudyStatus status;
    private final List<SamplingDay> days;
    private final List<StudyReopening> reopenings;
    private StudyCancellation cancellation;

    /**
     * Creates a planned study with its 8 days still without dates.
     *
     * @param id            identifier, or null if not saved yet
     * @param institutionId institution being characterized
     */
    public CharacterizationStudy(Long id, Long institutionId) {
        this.id = id;
        this.institutionId = institutionId;
        this.status = StudyStatus.PLANIFICADO;
        this.days = new ArrayList<>();
        this.reopenings = new ArrayList<>();
        IntStream.rangeClosed(1, SAMPLING_DAYS).forEach(day -> days.add(new SamplingDay(day, null)));
    }

    /**
     * Rebuilds a study read from the database with its stored status, dates and
     * reopenings, without repeating the lifecycle transitions (spec 002, plan R1).
     * Only the persistence layer must use it; new studies are created with the
     * constructor.
     *
     * @param id            study id
     * @param institutionId institution id
     * @param status        stored status
     * @param dates         the 8 stored dates in order (an element may be null)
     * @param reopenings    stored reopenings, oldest first
     * @return the rebuilt study
     * @throws InvalidDataException if there are not exactly 8 dates
     */
    public static CharacterizationStudy restore(Long id, Long institutionId, StudyStatus status,
                                                List<LocalDate> dates, List<StudyReopening> reopenings) {
        return restore(id, institutionId, status, dates, reopenings, null);
    }

    /**
     * Rebuilds a study read from the database, including its cancellation
     * (RN-018). Only the persistence layer must use it.
     *
     * @param id            study id
     * @param institutionId institution id
     * @param status        stored status
     * @param dates         the 8 stored dates in order (an element may be null)
     * @param reopenings    stored reopenings, oldest first
     * @param cancellation  stored cancellation, or null if the study was not cancelled
     * @return the rebuilt study
     * @throws InvalidDataException if there are not exactly 8 dates
     */
    public static CharacterizationStudy restore(Long id, Long institutionId, StudyStatus status,
                                                List<LocalDate> dates, List<StudyReopening> reopenings,
                                                StudyCancellation cancellation) {
        CharacterizationStudy study = new CharacterizationStudy(id, institutionId);
        if (dates == null || dates.size() != SAMPLING_DAYS) {
            throw new InvalidDataException("fechas",
                    "El estudio necesita exactamente " + SAMPLING_DAYS + " fechas, una por día de muestreo.");
        }
        for (int i = 0; i < SAMPLING_DAYS; i++) {
            study.days.get(i).setDate(dates.get(i));
        }
        study.status = status;
        if (reopenings != null) {
            study.reopenings.addAll(reopenings);
        }
        study.cancellation = cancellation;
        return study;
    }

    /**
     * Assigns the dates of the 8 days at once, in order from day 1 to day 8.
     * Their order is checked by {@code StudyValidator} (RN-011).
     *
     * @param dates exactly 8 dates
     * @throws InvalidDataException if there are not exactly 8 dates
     */
    public void scheduleDates(List<LocalDate> dates) {
        if (dates == null || dates.size() != SAMPLING_DAYS) {
            throw new InvalidDataException("fechas",
                    "El estudio necesita exactamente " + SAMPLING_DAYS + " fechas, una por día de muestreo.");
        }
        for (int i = 0; i < SAMPLING_DAYS; i++) {
            days.get(i).setDate(dates.get(i));
        }
    }

    /**
     * Returns one of the sampling days.
     *
     * @param dayNumber day number, 1 to 8
     * @return the sampling day
     * @throws InvalidDataException if the day number is outside 1 to 8
     */
    public SamplingDay getDay(int dayNumber) {
        if (dayNumber < 1 || dayNumber > SAMPLING_DAYS) {
            throw new InvalidDataException("dia", "El día de muestreo debe estar entre 1 y " + SAMPLING_DAYS + ".");
        }
        return days.get(dayNumber - 1);
    }

    /**
     * Indicates whether a day is excluded from the baseline calculations (RN-012).
     *
     * @param dayNumber day number, 1 to 8
     * @return true for day 1
     */
    public boolean isDayExcluded(int dayNumber) {
        return getDay(dayNumber).isExcluded();
    }

    /**
     * Returns the numbers of the days that enter the baseline calculations.
     *
     * @return days 2 to 8 (n = 7)
     */
    public List<Integer> getIncludedDayNumbers() {
        return days.stream().filter(day -> !day.isExcluded()).map(SamplingDay::getDayNumber).toList();
    }

    /**
     * Indicates whether the 8 days already have dates.
     *
     * @return true if every day is scheduled
     */
    public boolean hasAllDatesScheduled() {
        return days.stream().allMatch(SamplingDay::isScheduled);
    }

    /**
     * Returns the date of day 1.
     *
     * @return start date, or null if day 1 has no date yet
     */
    public LocalDate getStartDate() {
        return days.get(0).getDate();
    }

    /**
     * Returns the date of day 8.
     *
     * @return end date, or null if day 8 has no date yet
     */
    public LocalDate getEndDate() {
        return days.get(SAMPLING_DAYS - 1).getDate();
    }

    /**
     * Indicates whether the date ranges of two studies overlap (inclusive).
     * Studies without start or end date do not overlap with anything.
     *
     * @param other the other study
     * @return true if both ranges share at least one date
     */
    public boolean overlaps(CharacterizationStudy other) {
        if (other == null || getStartDate() == null || getEndDate() == null
                || other.getStartDate() == null || other.getEndDate() == null) {
            return false;
        }
        return !getStartDate().isAfter(other.getEndDate()) && !other.getStartDate().isAfter(getEndDate());
    }

    /**
     * Moves the study to another status, one step at a time (RN-014). The
     * conditions of each step (RN-015, RN-016) are checked beforehand by
     * {@code StudyValidator}.
     *
     * @param target next status
     * @throws BusinessRuleException if the transition is not allowed
     */
    public void changeStatus(StudyStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new BusinessRuleException("El estudio no puede pasar de " + status.getDisplayName()
                    + " a " + (target == null ? "un estado vacío" : target.getDisplayName()) + ".");
        }
        this.status = target;
    }

    /**
     * Reopens a closed study for data cleaning and records who did it, why and
     * when (RN-017). The permission of the user is checked by the caller.
     *
     * @param userId identifier of the teacher reopening the study
     * @param reason reason for reopening
     * @param at     date and time of the reopening
     * @throws BusinessRuleException if the study is not closed
     * @throws InvalidDataException  if the reason is missing
     */
    public void reopen(Long userId, String reason, LocalDateTime at) {
        if (status != StudyStatus.CERRADO) {
            throw new BusinessRuleException("Solo se puede reabrir un estudio cerrado.");
        }
        if (reason == null || reason.isBlank()) {
            throw new InvalidDataException("motivo", "Debe indicar el motivo para reabrir el estudio.");
        }
        changeStatus(StudyStatus.EN_DEPURACION);
        reopenings.add(new StudyReopening(userId, reason.trim(), at));
    }

    /**
     * Cancels a study being executed or cleaned and records who did it, why and
     * when (RN-018). Its data is kept. The permission of the user is checked by
     * the caller.
     *
     * @param userId identifier of the teacher cancelling the study
     * @param reason reason for the cancellation
     * @param at     date and time of the cancellation
     * @throws BusinessRuleException if the study is planned, closed or already cancelled
     * @throws InvalidDataException  if the reason is missing
     */
    public void cancel(Long userId, String reason, LocalDateTime at) {
        if (!status.allowsCancellation()) {
            throw new BusinessRuleException("Solo se puede cancelar un estudio en ejecución o en depuración. "
                    + "Este estudio está " + status.getDisplayName().toLowerCase() + ".");
        }
        if (reason == null || reason.isBlank()) {
            throw new InvalidDataException("motivo", "Debe indicar el motivo para cancelar el estudio.");
        }
        this.status = StudyStatus.CANCELADO;
        this.cancellation = new StudyCancellation(userId, reason.trim(), at);
    }

    /**
     * Checks that the study may be deleted: only while it is planned, because
     * it has no field data yet (RN-019).
     *
     * @throws BusinessRuleException if the study is not planned
     */
    public void requireDeletable() {
        if (status != StudyStatus.PLANIFICADO) {
            throw new BusinessRuleException("Solo se puede eliminar un estudio planificado. Este estudio está "
                    + status.getDisplayName().toLowerCase() + (status.allowsCancellation()
                    ? ": si ya no se va a usar, cancélelo." : "."));
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public StudyStatus getStatus() {
        return status;
    }

    /**
     * Returns the 8 sampling days in order.
     *
     * @return unmodifiable list of days
     */
    public List<SamplingDay> getDays() {
        return Collections.unmodifiableList(days);
    }

    /**
     * Returns the history of reopenings of the study.
     *
     * @return unmodifiable list of reopenings
     */
    public List<StudyReopening> getReopenings() {
        return Collections.unmodifiableList(reopenings);
    }

    /**
     * Returns the cancellation of the study.
     *
     * @return who, why and when it was cancelled, or null if it was not cancelled
     */
    public StudyCancellation getCancellation() {
        return cancellation;
    }
}
