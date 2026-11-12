package com.ecopoint.service;

import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.AlertFactory;
import com.ecopoint.model.CharacterizationSettings;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.Correction;
import com.ecopoint.model.DensityMeasurement;
import com.ecopoint.model.Institution;
import com.ecopoint.model.QualityStatus;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.QuarteringResult;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftPopulation;
import com.ecopoint.model.User;
import com.ecopoint.model.VerifiableRecord;
import com.ecopoint.model.WasteBag;
import com.ecopoint.persistence.AlertRepository;
import com.ecopoint.persistence.CorrectionRepository;
import com.ecopoint.persistence.DensityRepository;
import com.ecopoint.persistence.InstitutionRepository;
import com.ecopoint.persistence.PendingRecord;
import com.ecopoint.persistence.QuarteringRepository;
import com.ecopoint.persistence.ShiftRepository;
import com.ecopoint.persistence.StudyRepository;
import com.ecopoint.persistence.VerificationRepository;
import com.ecopoint.persistence.WasteBagRepository;
import com.ecopoint.validation.DensityValidator;
import com.ecopoint.validation.QuarteringValidator;
import com.ecopoint.validation.ShiftValidator;
import com.ecopoint.validation.VerificationValidator;
import com.ecopoint.validation.WasteBagValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Double entry: verify a record against the paper form or correct it, keeping
 * the old value in the correction history (RN-060 to RN-063; spec 003, section 5.3).
 *
 * <p>Fields that can be corrected: population {@code students}, {@code teachers},
 * {@code staff}; bag {@code grossWeight}, {@code tare}, {@code volume}; density
 * {@code containerVolume}, {@code netWeight}; quartering {@code sampleWeight} and
 * {@code result:<subcategory id>} (re-weighing, RN-052).</p>
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Service
public class VerificationService {

    private final VerificationRepository verifications;
    private final CorrectionRepository corrections;
    private final ShiftRepository shifts;
    private final StudyRepository studies;
    private final WasteBagRepository bags;
    private final QuarteringRepository quarterings;
    private final DensityRepository densities;
    private final InstitutionRepository institutions;
    private final AlertRepository alerts;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param verifications verification repository
     * @param corrections   correction history repository
     * @param shifts        shift repository
     * @param studies       study repository
     * @param bags          bag repository
     * @param quarterings   quartering repository
     * @param densities     density repository
     * @param institutions  institution repository
     * @param alerts        alert repository
     * @param clock         clock that dates verifications and corrections
     */
    public VerificationService(VerificationRepository verifications, CorrectionRepository corrections,
                               ShiftRepository shifts, StudyRepository studies, WasteBagRepository bags,
                               QuarteringRepository quarterings, DensityRepository densities,
                               InstitutionRepository institutions, AlertRepository alerts, Clock clock) {
        this.verifications = verifications;
        this.corrections = corrections;
        this.shifts = shifts;
        this.studies = studies;
        this.bags = bags;
        this.quarterings = quarterings;
        this.densities = densities;
        this.institutions = institutions;
        this.alerts = alerts;
        this.clock = clock;
    }

    /**
     * Lists the records of a study still pending verification.
     *
     * @param studyId study id
     * @param shiftId shift to keep, or null for the whole study
     * @return pending records ordered by shift
     */
    public List<PendingRecord> pending(Long studyId, Long shiftId) {
        return verifications.findPendingByStudy(studyId).stream()
                .filter(pending -> shiftId == null || Objects.equals(pending.shiftId(), shiftId))
                .toList();
    }

    /**
     * Returns the correction history of a record (RN-063).
     *
     * @param kind     kind of record
     * @param recordId record id
     * @return corrections, oldest first
     */
    public List<Correction> history(RecordKind kind, Long recordId) {
        return corrections.findByRecord(kind.getRecordType(), recordId);
    }

    /**
     * Marks a record as matching the paper form. Only another person than the
     * one who typed it may do it (RN-062).
     *
     * @param actor    user that verifies
     * @param kind     kind of record
     * @param recordId record id
     * @return the verified record
     */
    @Transactional
    public VerifiableRecord verify(User actor, RecordKind kind, Long recordId) {
        Located located = locate(kind, recordId);
        VerificationValidator.validateStudyAllowsVerification(located.study());
        VerificationValidator.validateVerifier(located.record(), actor);
        located.record().getVerification().markVerified(actor.getId(), LocalDateTime.now(clock));
        verifications.updateVerification(located.record());
        return located.record();
    }

    /**
     * Corrects a record that does not match the paper form: the new values
     * replace the old ones, each change is kept in the history with its reason
     * and the record becomes CORREGIDO (RN-063), all in one transaction.
     *
     * @param actor     user that corrects
     * @param kind      kind of record
     * @param recordId  record id
     * @param newValues new value of each corrected field
     * @param reason    reason of the correction
     * @return the corrected record
     */
    @Transactional
    public VerifiableRecord correct(User actor, RecordKind kind, Long recordId, Map<String, BigDecimal> newValues,
                                    String reason) {
        Located located = locate(kind, recordId);
        VerificationValidator.validateStudyAllowsVerification(located.study());
        VerificationValidator.validateCorrection(located.record(), actor, reason);
        LocalDateTime now = LocalDateTime.now(clock);
        List<Correction> changes = new ArrayList<>();
        (newValues == null ? Map.<String, BigDecimal>of() : newValues).forEach((field, value) ->
                apply(located, field, value, reason, actor, now, changes));
        if (changes.isEmpty()) {
            throw new InvalidDataException("valores", "No hay cambios que corregir: los valores son iguales.");
        }
        revalidateAndSave(located, now);
        located.record().getVerification().markCorrected(actor.getId(), now);
        verifications.updateVerification(located.record());
        changes.forEach(corrections::save);
        return located.record();
    }

    private void apply(Located located, String field, BigDecimal value, String reason, User actor,
                       LocalDateTime now, List<Correction> changes) {
        if (value == null) {
            throw new InvalidDataException(field, "El valor corregido no puede estar vacío.");
        }
        VerifiableRecord target = located.record();
        if (target instanceof ShiftPopulation population) {
            applyPopulation(population, field, value, changes, reason, actor, now);
        } else if (target instanceof WasteBag bag) {
            applyBag(bag, field, value, changes, reason, actor, now);
        } else if (target instanceof DensityMeasurement density) {
            applyDensity(density, field, value, changes, reason, actor, now);
        } else if (target instanceof Quartering quartering) {
            applyQuartering(quartering, field, value, changes, reason, actor, now);
        } else {
            throw new InvalidDataException("registro", "Tipo de registro desconocido.");
        }
    }

    private static void applyPopulation(ShiftPopulation population, String field, BigDecimal value,
                                        List<Correction> changes, String reason, User actor, LocalDateTime now) {
        int count = wholeNumber(field, value);
        switch (field) {
            case "students" -> change(changes, population, field, population.getStudents(), count,
                    population::setStudents, reason, actor, now);
            case "teachers" -> change(changes, population, field, population.getTeachers(), count,
                    population::setTeachers, reason, actor, now);
            case "staff" -> change(changes, population, field, population.getStaff(), count,
                    population::setStaff, reason, actor, now);
            default -> throw unknownField(field);
        }
    }

    private static void applyBag(WasteBag bag, String field, BigDecimal value, List<Correction> changes,
                                 String reason, User actor, LocalDateTime now) {
        switch (field) {
            case "grossWeight" -> change(changes, bag, field, bag.getGrossWeight(), value, bag::setGrossWeight,
                    reason, actor, now);
            case "tare" -> change(changes, bag, field, bag.getTare(), value, bag::setTare, reason, actor, now);
            case "volume" -> change(changes, bag, field, bag.getVolume(), value, bag::setVolume, reason, actor, now);
            default -> throw unknownField(field);
        }
    }

    private static void applyDensity(DensityMeasurement density, String field, BigDecimal value,
                                     List<Correction> changes, String reason, User actor, LocalDateTime now) {
        switch (field) {
            case "containerVolume" -> change(changes, density, field, density.getContainerVolume(), value,
                    density::setContainerVolume, reason, actor, now);
            case "netWeight" -> change(changes, density, field, density.getNetWeight(), value,
                    density::setNetWeight, reason, actor, now);
            default -> throw unknownField(field);
        }
    }

    private static void applyQuartering(Quartering quartering, String field, BigDecimal value,
                                        List<Correction> changes, String reason, User actor, LocalDateTime now) {
        if ("sampleWeight".equals(field)) {
            change(changes, quartering, field, quartering.getSampleWeight(), value, quartering::setSampleWeight,
                    reason, actor, now);
            return;
        }
        if (!field.startsWith("result:")) {
            throw unknownField(field);
        }
        Long categoryId = parseId(field);
        QuarteringResult result = quartering.getResults().stream()
                .filter(found -> Objects.equals(found.getCategoryId(), categoryId))
                .findFirst()
                .orElseThrow(() -> new InvalidDataException(field,
                        "El cuarteo no tiene resultado para la subcategoría " + categoryId + "."));
        change(changes, quartering, field, result.getWeight(), value, result::setWeight, reason, actor, now);
    }

    private static <T> void change(List<Correction> changes, VerifiableRecord record, String field, T oldValue,
                                   T newValue, Consumer<T> setter, String reason, User actor, LocalDateTime now) {
        if (sameValue(oldValue, newValue)) {
            return;
        }
        changes.add(Correction.of(record, field, plain(oldValue), plain(newValue), reason, actor.getId(), now));
        setter.accept(newValue);
    }

    private void revalidateAndSave(Located located, LocalDateTime now) {
        VerifiableRecord target = located.record();
        if (target instanceof ShiftPopulation population) {
            ShiftValidator.validatePopulation(population);
            shifts.save(located.shift());
        } else if (target instanceof WasteBag bag) {
            saveBag(located, bag, now);
        } else if (target instanceof DensityMeasurement density) {
            DensityValidator.validate(density);
            densities.save(density);
        } else if (target instanceof Quartering quartering) {
            saveQuartering(located, quartering, now);
        } else {
            throw new InvalidDataException("registro", "Tipo de registro desconocido.");
        }
    }

    private void saveBag(Located located, WasteBag bag, LocalDateTime now) {
        WasteBagValidator.validate(bag, null);
        boolean wasAtypical = bag.isAtypical();
        bag.setAtypical(WasteBagValidator.isAtypical(bag, settingsOf(located.study())));
        bags.save(located.shift().getId(), bag);
        if (bag.isAtypical() && !wasAtypical) {
            alerts.save(AlertFactory.forAtypicalBag(bag, now));
        }
    }

    private void saveQuartering(Located located, Quartering quartering, LocalDateTime now) {
        CharacterizationSettings settings = settingsOf(located.study());
        QuarteringValidator.validate(quartering, settings);
        QualityStatus before = quartering.getQualityStatus();
        quartering.calculateCompositionPercentages();
        quartering.evaluateQuality(settings.qualityTolerancePercent());
        quarterings.save(quartering);
        if (quartering.getQualityStatus() == QualityStatus.NO_CONFORME && before != QualityStatus.NO_CONFORME) {
            alerts.save(AlertFactory.forNonConformingQuartering(quartering, located.shift().getCode(), now));
        }
    }

    private Located locate(RecordKind kind, Long recordId) {
        Long shiftId = verifications.findShiftOfRecord(kind.getRecordType(), recordId)
                .orElseThrow(() -> new ResourceNotFoundException(kind.getRecordType(), recordId));
        SamplingShift shift = shifts.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Turno", shiftId));
        CharacterizationStudy study = studies.findById(shift.getStudyId())
                .orElseThrow(() -> new ResourceNotFoundException("Estudio", shift.getStudyId()));
        VerifiableRecord found = switch (kind) {
            case POPULATION -> shift.getPopulation();
            case QUARTERING -> shift.getQuartering();
            case DENSITY -> shift.getDensity();
            case BAG -> shift.getBags().stream()
                    .filter(bag -> Objects.equals(bag.getId(), recordId))
                    .findFirst().orElse(null);
        };
        if (found == null) {
            throw new ResourceNotFoundException(kind.getRecordType(), recordId);
        }
        return new Located(found, shift, study);
    }

    private CharacterizationSettings settingsOf(CharacterizationStudy study) {
        return institutions.findById(study.getInstitutionId())
                .map(Institution::getSettings)
                .orElseGet(CharacterizationSettings::defaults);
    }

    private static boolean sameValue(Object oldValue, Object newValue) {
        if (oldValue instanceof BigDecimal oldDecimal && newValue instanceof BigDecimal newDecimal) {
            return oldDecimal.compareTo(newDecimal) == 0;
        }
        return Objects.equals(oldValue, newValue);
    }

    private static Object plain(Object value) {
        return value instanceof BigDecimal decimal ? decimal.toPlainString() : value;
    }

    private static int wholeNumber(String field, BigDecimal value) {
        try {
            return value.intValueExact();
        } catch (ArithmeticException notWhole) {
            throw new InvalidDataException(field, "Debe ser un número entero de personas.");
        }
    }

    private static Long parseId(String field) {
        try {
            return Long.valueOf(field.substring("result:".length()));
        } catch (NumberFormatException notANumber) {
            throw unknownField(field);
        }
    }

    private static InvalidDataException unknownField(String field) {
        return new InvalidDataException(field, "Este campo no se puede corregir.");
    }

    /** A record together with its shift and study. */
    private record Located(VerifiableRecord record, SamplingShift shift, CharacterizationStudy study) {
    }
}
