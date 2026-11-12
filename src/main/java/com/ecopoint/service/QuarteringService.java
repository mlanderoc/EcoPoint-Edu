package com.ecopoint.service;

import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.AlertFactory;
import com.ecopoint.model.CharacterizationSettings;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.Institution;
import com.ecopoint.model.QualityStatus;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.QuarteringResult;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.User;
import com.ecopoint.model.VerificationInfo;
import com.ecopoint.model.WasteCategory;
import com.ecopoint.persistence.AlertRepository;
import com.ecopoint.persistence.InstitutionRepository;
import com.ecopoint.persistence.QuarteringRepository;
import com.ecopoint.persistence.ShiftRepository;
import com.ecopoint.persistence.StudyRepository;
import com.ecopoint.persistence.WasteCategoryRepository;
import com.ecopoint.validation.QuarteringValidator;
import com.ecopoint.validation.ShiftValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Quartering of a shift with its subcategory results and its 2 % quality
 * control (RN-050 to RN-056, RN-070; spec 003, section 5.3).
 *
 * @author craulgomez
 * @version 1.0
 */
@Service
public class QuarteringService {

    /**
     * Weight of one subcategory in a quartering request.
     *
     * @param categoryId subcategory id
     * @param weight     weight, in kg
     */
    public record ResultInput(Long categoryId, BigDecimal weight) {
    }

    private final QuarteringRepository quarterings;
    private final ShiftRepository shifts;
    private final StudyRepository studies;
    private final InstitutionRepository institutions;
    private final WasteCategoryRepository categories;
    private final AlertRepository alerts;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param quarterings  quartering repository
     * @param shifts       shift repository
     * @param studies      study repository
     * @param institutions institution repository
     * @param categories   subcategory repository
     * @param alerts       alert repository
     * @param clock        clock that dates the records and alerts
     */
    public QuarteringService(QuarteringRepository quarterings, ShiftRepository shifts, StudyRepository studies,
                             InstitutionRepository institutions, WasteCategoryRepository categories,
                             AlertRepository alerts, Clock clock) {
        this.quarterings = quarterings;
        this.shifts = shifts;
        this.studies = studies;
        this.institutions = institutions;
        this.categories = categories;
        this.alerts = alerts;
        this.clock = clock;
    }

    /**
     * Finds the quartering of a shift.
     *
     * @param shiftId shift id
     * @return the quartering
     * @throws ResourceNotFoundException if the shift has no quartering
     */
    public Quartering get(Long shiftId) {
        return quarterings.findByShift(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuarteo del turno", shiftId));
    }

    /**
     * Registers the quartering of a shift. One outside the 2 % tolerance is
     * saved as NO_CONFORME with its alert, so the crew can re-weigh (RN-052, RN-070).
     *
     * @param actor        user that types it
     * @param shiftId      shift id
     * @param sampleWeight weight of the subsample, in kg
     * @param inputs       weight of each subcategory
     * @return the saved quartering with its quality status
     */
    @Transactional
    public Quartering register(User actor, Long shiftId, BigDecimal sampleWeight, List<ResultInput> inputs) {
        SamplingShift shift = shifts.findById(shiftId).orElseThrow(() -> new ResourceNotFoundException("Turno", shiftId));
        CharacterizationStudy study = studies.findById(shift.getStudyId())
                .orElseThrow(() -> new ResourceNotFoundException("Estudio", shift.getStudyId()));
        LocalDateTime now = LocalDateTime.now(clock);
        Quartering quartering = new Quartering(null, shiftId, shift.calculateTotalNetWeight(), sampleWeight,
                new VerificationInfo(actor == null ? null : actor.getId(), now));
        Map<Long, WasteCategory> catalog = categories.findAllAsMap();
        (inputs == null ? List.<ResultInput>of() : inputs)
                .forEach(input -> quartering.addResult(resultOf(input, catalog)));

        QuarteringValidator.validatePermissions(quartering, actor);
        ShiftValidator.validateOpenForRegistration(shift, study);
        QuarteringValidator.validateNoExistingQuartering(shift);
        QuarteringValidator.validateCategories(quartering, catalog);
        CharacterizationSettings settings = settingsOf(study);
        QuarteringValidator.validate(quartering, settings);

        quartering.calculateCompositionPercentages();
        quartering.evaluateQuality(settings.qualityTolerancePercent());
        shift.registerQuartering(quartering);
        Quartering saved = quarterings.save(quartering);
        if (saved.getQualityStatus() == QualityStatus.NO_CONFORME) {
            alerts.save(AlertFactory.forNonConformingQuartering(saved, shift.getCode(), now));
        }
        return saved;
    }

    /**
     * Annuls a non-conforming quartering with a reason, so it no longer blocks
     * closing the study (RN-052).
     *
     * @param actor   user that annuls it
     * @param shiftId shift id
     * @param reason  reason of the annulment
     * @return the annulled quartering
     */
    @Transactional
    public Quartering annul(User actor, Long shiftId, String reason) {
        Quartering quartering = get(shiftId);
        QuarteringValidator.validateAnnul(quartering, actor);
        quartering.annul(reason, actor.getId(), LocalDateTime.now(clock));
        return quarterings.save(quartering);
    }

    private static QuarteringResult resultOf(ResultInput input, Map<Long, WasteCategory> catalog) {
        WasteCategory category = catalog.get(input.categoryId());
        if (category == null) {
            return new QuarteringResult(input.categoryId(), "#" + input.categoryId(), null, input.weight());
        }
        return QuarteringResult.of(category, input.weight());
    }

    private CharacterizationSettings settingsOf(CharacterizationStudy study) {
        return institutions.findById(study.getInstitutionId())
                .map(Institution::getSettings)
                .orElseGet(CharacterizationSettings::defaults);
    }
}
