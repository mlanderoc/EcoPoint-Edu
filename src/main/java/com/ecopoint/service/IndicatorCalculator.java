package com.ecopoint.service;

import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.QualityStatus;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.QuarteringResult;
import com.ecopoint.model.SamplingDay;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftType;
import com.ecopoint.model.StudyStatus;
import com.ecopoint.model.VerifiableRecord;
import com.ecopoint.model.VerificationStatus;
import com.ecopoint.model.WasteBag;
import com.ecopoint.model.WasteGroup;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Formulas of the baseline indicators (spec 006, section 5.2). Only the days
 * that are not excluded enter (R-500), annulled quarterings are left out
 * (R-501) and the composition is weighted by mass (R-510). Nothing is rounded
 * here (R-511).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public final class IndicatorCalculator {

    private static final MathContext PRECISION = MathContext.DECIMAL128;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int SHIFTS_PER_DAY = ShiftType.values().length;

    private IndicatorCalculator() {
    }

    /**
     * Calculates every indicator of a study.
     *
     * @param study   study with its days
     * @param shifts  shifts of the study with everything recorded in them
     * @param sources sources of the institution by id
     * @return the indicators with full precision
     */
    public static StudyIndicators calculate(CharacterizationStudy study, List<SamplingShift> shifts,
                                            Map<Long, GenerationSource> sources) {
        Set<Integer> countedDays = study.getDays().stream().filter(day -> !day.isExcluded())
                .map(SamplingDay::getDayNumber).collect(Collectors.toSet());
        List<SamplingShift> counted = shifts.stream().filter(shift -> countedDays.contains(shift.getDayNumber())).toList();
        int dayCount = countedDays.size();

        List<StudyIndicators.DailyGeneration> days = study.getDays().stream()
                .map(day -> dailyGeneration(day, shifts)).toList();
        List<BigDecimal> dailyPpc = days.stream().filter(day -> !day.excluded())
                .map(StudyIndicators.DailyGeneration::ppc).filter(Objects::nonNull).toList();
        BigDecimal averageGeneration = divide(sum(days.stream().filter(day -> !day.excluded())
                .map(StudyIndicators.DailyGeneration::generation)), dayCount);
        BigDecimal averagePpc = average(dailyPpc);
        BigDecimal deviation = sampleStandardDeviation(dailyPpc);
        StudyIndicators.Composition composition = composition(counted, dayCount * SHIFTS_PER_DAY);
        BigDecimal recoverable = recoverableFraction(composition);

        StudyIndicators.Summary summary = new StudyIndicators.Summary(averageGeneration, averagePpc, deviation,
                percentageOf(deviation, averagePpc), averageDensity(counted), recoverable,
                recoverable == null || averageGeneration == null ? null
                        : averageGeneration.multiply(recoverable).divide(HUNDRED, PRECISION),
                dayCount);

        return new StudyIndicators(study.getId(), study.getStatus(), study.getStatus() == StudyStatus.CERRADO,
                summary, days, sourceGeneration(counted, sources, dayCount),
                Arrays.stream(ShiftType.values()).map(type -> shiftTypeComparison(type, counted, dayCount)).toList(),
                composition, quality(shifts));
    }

    private static StudyIndicators.DailyGeneration dailyGeneration(SamplingDay day, List<SamplingShift> shifts) {
        List<SamplingShift> ofDay = shifts.stream().filter(shift -> shift.getDayNumber() == day.getDayNumber()).toList();
        BigDecimal morning = generationOf(ofDay, ShiftType.MANANA);
        BigDecimal afternoon = generationOf(ofDay, ShiftType.TARDE);
        BigDecimal generation = morning.add(afternoon);
        int population = ofDay.stream().filter(SamplingShift::hasPopulation)
                .mapToInt(shift -> shift.getPopulation().getTotal()).sum();
        return new StudyIndicators.DailyGeneration(day.getDayNumber(), day.getDate(), day.isExcluded(), morning,
                afternoon, generation, population, population == 0 ? null : divide(generation, population));
    }

    private static BigDecimal generationOf(List<SamplingShift> shifts, ShiftType type) {
        return sum(shifts.stream().filter(shift -> shift.getType() == type).map(SamplingShift::calculateTotalNetWeight));
    }

    private static List<StudyIndicators.SourceGeneration> sourceGeneration(List<SamplingShift> counted,
                                                                          Map<Long, GenerationSource> sources,
                                                                          int dayCount) {
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        Map<Long, String> codes = new LinkedHashMap<>();
        counted.stream().flatMap(shift -> shift.getBags().stream()).forEach(bag -> {
            totals.merge(bag.getSourceId(), bag.getNetWeight(), BigDecimal::add);
            codes.putIfAbsent(bag.getSourceId(), bag.getSourceCode());
        });
        return totals.entrySet().stream()
                .map(entry -> {
                    GenerationSource source = sources.get(entry.getKey());
                    return new StudyIndicators.SourceGeneration(entry.getKey(),
                            source == null ? codes.get(entry.getKey()) : source.getCode(),
                            source == null ? codes.get(entry.getKey()) : source.getName(),
                            divide(entry.getValue(), dayCount));
                })
                .sorted(Comparator.comparing(StudyIndicators.SourceGeneration::averageDailyGeneration).reversed())
                .toList();
    }

    private static StudyIndicators.ShiftTypeComparison shiftTypeComparison(ShiftType type, List<SamplingShift> counted,
                                                                           int dayCount) {
        List<SamplingShift> ofType = counted.stream().filter(shift -> shift.getType() == type).toList();
        List<SamplingShift> withPopulation = ofType.stream()
                .filter(shift -> shift.hasPopulation() && shift.getPopulation().getTotal() > 0).toList();
        List<BigDecimal> ppc = withPopulation.stream()
                .map(shift -> divide(shift.calculateTotalNetWeight(), shift.getPopulation().getTotal())).toList();
        return new StudyIndicators.ShiftTypeComparison(type,
                divide(sum(ofType.stream().map(SamplingShift::calculateTotalNetWeight)), dayCount),
                average(withPopulation.stream().map(shift -> BigDecimal.valueOf(shift.getPopulation().getTotal())).toList()),
                average(ppc), averageDensity(ofType), composition(ofType, dayCount));
    }

    private static BigDecimal averageDensity(List<SamplingShift> shifts) {
        return average(shifts.stream().filter(SamplingShift::hasDensity)
                .map(shift -> shift.getDensity().getDensity()).filter(Objects::nonNull).toList());
    }

    /**
     * Composition weighted by mass over the valid quarterings of the shifts (R-510).
     *
     * @param shifts         shifts whose quarterings enter
     * @param expectedShifts shifts that should have a quartering
     * @return the composition
     */
    static StudyIndicators.Composition composition(List<SamplingShift> shifts, int expectedShifts) {
        List<Quartering> valid = shifts.stream().filter(SamplingShift::hasQuartering).map(SamplingShift::getQuartering)
                .filter(quartering -> quartering.getQualityStatus() != QualityStatus.ANULADO).toList();
        List<QuarteringResult> results = valid.stream().flatMap(quartering -> quartering.getResults().stream()).toList();
        BigDecimal total = sum(results.stream().map(QuarteringResult::getWeight));

        List<StudyIndicators.GroupShare> groups = Arrays.stream(WasteGroup.values()).map(group -> {
            BigDecimal weight = sum(results.stream().filter(result -> result.getGroup() == group)
                    .map(QuarteringResult::getWeight));
            return new StudyIndicators.GroupShare(group, weight, percentageOf(weight, total));
        }).toList();

        Map<Long, List<QuarteringResult>> byCategory = results.stream()
                .collect(Collectors.groupingBy(QuarteringResult::getCategoryId, LinkedHashMap::new, Collectors.toList()));
        List<StudyIndicators.SubcategoryShare> subcategories = new ArrayList<>();
        byCategory.forEach((categoryId, ofCategory) -> {
            BigDecimal weight = sum(ofCategory.stream().map(QuarteringResult::getWeight));
            QuarteringResult first = ofCategory.get(0);
            subcategories.add(new StudyIndicators.SubcategoryShare(categoryId, first.getCategoryName(),
                    first.getGroup(), weight, percentageOf(weight, total)));
        });
        subcategories.sort(Comparator.comparing(StudyIndicators.SubcategoryShare::weight).reversed());
        return new StudyIndicators.Composition(groups, subcategories, valid.size(), expectedShifts);
    }

    private static BigDecimal recoverableFraction(StudyIndicators.Composition composition) {
        List<BigDecimal> recoverable = composition.groups().stream().filter(share -> share.group().isRecoverable())
                .map(StudyIndicators.GroupShare::percentage).toList();
        return recoverable.contains(null) ? null : sum(recoverable.stream());
    }

    private static StudyIndicators.DataQuality quality(List<SamplingShift> shifts) {
        List<VerifiableRecord> records = shifts.stream().flatMap(IndicatorCalculator::recordsOf).toList();
        int verified = (int) records.stream()
                .filter(record -> record.getVerification().getStatus() != VerificationStatus.PENDIENTE).count();
        int corrected = (int) records.stream()
                .filter(record -> record.getVerification().getStatus() == VerificationStatus.CORREGIDO).count();
        int nonConforming = (int) shifts.stream().filter(SamplingShift::hasQuartering)
                .filter(shift -> shift.getQuartering().getQualityStatus() == QualityStatus.NO_CONFORME).count();
        return new StudyIndicators.DataQuality(records.size(), verified,
                percentageOf(BigDecimal.valueOf(verified), BigDecimal.valueOf(records.size())), corrected, nonConforming);
    }

    private static Stream<VerifiableRecord> recordsOf(SamplingShift shift) {
        Stream<WasteBag> bags = shift.getBags().stream();
        return Stream.concat(Stream.concat(Stream.ofNullable(shift.getPopulation()), bags),
                Stream.concat(Stream.ofNullable(shift.getQuartering()), Stream.ofNullable(shift.getDensity())));
    }

    /**
     * Sample standard deviation (n − 1), as decided in question P3.
     *
     * @param values values of the sample
     * @return the deviation, or null with fewer than two values
     */
    static BigDecimal sampleStandardDeviation(List<BigDecimal> values) {
        if (values.size() < 2) {
            return null;
        }
        BigDecimal mean = average(values);
        BigDecimal squares = sum(values.stream().map(value -> value.subtract(mean).pow(2)));
        return squares.divide(BigDecimal.valueOf(values.size() - 1L), PRECISION).sqrt(PRECISION);
    }

    private static BigDecimal average(List<BigDecimal> values) {
        return values.isEmpty() ? null : divide(sum(values.stream()), values.size());
    }

    private static BigDecimal percentageOf(BigDecimal part, BigDecimal total) {
        if (part == null || total == null || total.signum() == 0) {
            return null;
        }
        return part.multiply(HUNDRED).divide(total, PRECISION);
    }

    private static BigDecimal divide(BigDecimal value, int divisor) {
        return divisor == 0 ? null : value.divide(BigDecimal.valueOf(divisor), PRECISION);
    }

    private static BigDecimal sum(Stream<BigDecimal> values) {
        return values.filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
