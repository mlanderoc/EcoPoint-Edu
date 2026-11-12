package com.ecopoint.service;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.AlertFactory;
import com.ecopoint.model.CharacterizationSettings;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.Institution;
import com.ecopoint.model.Permission;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.User;
import com.ecopoint.model.VerificationInfo;
import com.ecopoint.model.WasteBag;
import com.ecopoint.persistence.AlertRepository;
import com.ecopoint.persistence.GenerationSourceRepository;
import com.ecopoint.persistence.InstitutionRepository;
import com.ecopoint.persistence.ShiftRepository;
import com.ecopoint.persistence.StudyRepository;
import com.ecopoint.persistence.WasteBagRepository;
import com.ecopoint.validation.AccessPolicy;
import com.ecopoint.validation.GenerationSourceValidator;
import com.ecopoint.validation.ShiftValidator;
import com.ecopoint.validation.WasteBagValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Bags of a shift: registration with its label and its atypical alert, and
 * correction while it is still pending verification (RN-023, RN-030 to RN-037,
 * RN-071; spec 003, section 5.2; spec 004, R-343, R-344 and R-348).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Service
public class WasteBagService {

    private final WasteBagRepository bags;
    private final ShiftRepository shifts;
    private final StudyRepository studies;
    private final GenerationSourceRepository sources;
    private final InstitutionRepository institutions;
    private final AlertRepository alerts;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param bags         bag repository
     * @param shifts       shift repository
     * @param studies      study repository
     * @param sources      source repository
     * @param institutions institution repository
     * @param alerts       alert repository
     * @param clock        clock that dates the records and alerts
     */
    public WasteBagService(WasteBagRepository bags, ShiftRepository shifts, StudyRepository studies,
                           GenerationSourceRepository sources, InstitutionRepository institutions,
                           AlertRepository alerts, Clock clock) {
        this.bags = bags;
        this.shifts = shifts;
        this.studies = studies;
        this.sources = sources;
        this.institutions = institutions;
        this.alerts = alerts;
        this.clock = clock;
    }

    /**
     * Lists the bags of a shift.
     *
     * @param shiftId shift id
     * @return bags in the order they were registered
     */
    public List<WasteBag> list(Long shiftId) {
        shift(shiftId);
        return bags.findByShift(shiftId);
    }

    /**
     * Finds a bag.
     *
     * @param id bag id
     * @return the bag
     */
    public WasteBag get(Long id) {
        return bags.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bolsa", id));
    }

    /**
     * Returns the shift a bag belongs to.
     *
     * @param bagId bag id
     * @return the shift id
     * @throws ResourceNotFoundException if the bag does not exist
     */
    public Long shiftOf(Long bagId) {
        return bags.findShiftId(bagId).orElseThrow(() -> new ResourceNotFoundException("Bolsa", bagId));
    }

    /**
     * Registers a bag. The label is assigned by the shift; an atypical bag is
     * saved anyway together with its alert (RN-037, RN-071).
     *
     * @param actor       user that types it
     * @param shiftId     shift id
     * @param sourceId    source the waste comes from
     * @param grossWeight gross weight, in kg
     * @param tare        tare, in kg
     * @param volume      volume, in m³
     * @return the saved bag with its label, net weight, density and atypical mark
     */
    @Transactional
    public WasteBag register(User actor, Long shiftId, Long sourceId, BigDecimal grossWeight, BigDecimal tare,
                             BigDecimal volume) {
        AccessPolicy.require(actor, Permission.REGISTER_BAGS);
        SamplingShift shift = shift(shiftId);
        CharacterizationStudy study = study(shift);
        ShiftValidator.validateCanRegisterBag(shift, study);
        GenerationSource source = source(sourceId);
        GenerationSourceValidator.validateAvailableForRegistration(source);
        WasteBag bag = new WasteBag(null, sourceId, source.getCode(), grossWeight, tare, volume,
                new VerificationInfo(actor.getId(), LocalDateTime.now(clock)));
        WasteBagValidator.validate(bag, studies.findParticipatingSources(study.getId()));
        bag.setAtypical(WasteBagValidator.isAtypical(bag, settingsOf(study)));
        shift.addBag(bag);
        WasteBag saved = bags.save(shiftId, bag);
        raiseAlertIfAtypical(saved);
        return saved;
    }

    /**
     * Corrects the weights of a bag of an open shift while it is still pending
     * verification; after that it is corrected from the verification (R-348).
     *
     * @param actor       user that corrects it
     * @param bagId       bag id
     * @param grossWeight new gross weight, in kg
     * @param tare        new tare, in kg
     * @param volume      new volume, in m³
     * @return the saved bag
     */
    @Transactional
    public WasteBag correctPending(User actor, Long bagId, BigDecimal grossWeight, BigDecimal tare,
                                   BigDecimal volume) {
        AccessPolicy.require(actor, Permission.REGISTER_BAGS);
        WasteBag bag = get(bagId);
        Long shiftId = shiftOf(bagId);
        SamplingShift shift = shift(shiftId);
        CharacterizationStudy study = study(shift);
        ShiftValidator.validateOpenForRegistration(shift, study);
        if (!bag.isPendingVerification()) {
            throw new BusinessRuleException("La bolsa " + bag.getLabelText()
                    + " ya fue verificada: corríjala desde la verificación.");
        }
        boolean wasAtypical = bag.isAtypical();
        bag.setGrossWeight(grossWeight);
        bag.setTare(tare);
        bag.setVolume(volume);
        WasteBagValidator.validate(bag, studies.findParticipatingSources(study.getId()));
        bag.setAtypical(WasteBagValidator.isAtypical(bag, settingsOf(study)));
        WasteBag saved = bags.save(shiftId, bag);
        if (!wasAtypical) {
            raiseAlertIfAtypical(saved);
        }
        return saved;
    }

    private void raiseAlertIfAtypical(WasteBag bag) {
        if (bag.isAtypical()) {
            alerts.save(AlertFactory.forAtypicalBag(bag, LocalDateTime.now(clock)));
        }
    }

    private CharacterizationSettings settingsOf(CharacterizationStudy study) {
        return institutions.findById(study.getInstitutionId())
                .map(Institution::getSettings)
                .orElseGet(CharacterizationSettings::defaults);
    }

    private SamplingShift shift(Long shiftId) {
        return shifts.findById(shiftId).orElseThrow(() -> new ResourceNotFoundException("Turno", shiftId));
    }

    private CharacterizationStudy study(SamplingShift shift) {
        return studies.findById(shift.getStudyId())
                .orElseThrow(() -> new ResourceNotFoundException("Estudio", shift.getStudyId()));
    }

    private GenerationSource source(Long sourceId) {
        if (sourceId == null) {
            throw new InvalidDataException("fuente", "Debe elegir la fuente de la que proviene la bolsa.");
        }
        return sources.findById(sourceId).orElseThrow(() -> new ResourceNotFoundException("Fuente de generación",
                sourceId));
    }
}
