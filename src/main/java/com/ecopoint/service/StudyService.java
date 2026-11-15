package com.ecopoint.service;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.Permission;
import com.ecopoint.model.StudyStatus;
import com.ecopoint.model.User;
import com.ecopoint.persistence.GenerationSourceRepository;
import com.ecopoint.persistence.InstitutionRepository;
import com.ecopoint.persistence.StudyRepository;
import com.ecopoint.validation.AccessPolicy;
import com.ecopoint.validation.StudyValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Characterization studies: their 8 days, participating sources and status
 * (RN-010 to RN-017; spec 003, section 5.2; spec 004, R-330 to R-333).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Service
public class StudyService {

    private final StudyRepository studies;
    private final InstitutionRepository institutions;
    private final GenerationSourceRepository sources;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param studies      study repository
     * @param institutions institution repository
     * @param sources      source repository
     * @param clock        clock that dates the reopenings
     */
    public StudyService(StudyRepository studies, InstitutionRepository institutions,
                        GenerationSourceRepository sources, Clock clock) {
        this.studies = studies;
        this.institutions = institutions;
        this.sources = sources;
        this.clock = clock;
    }

    /**
     * Lists the studies of an institution.
     *
     * @param institutionId institution id
     * @return studies
     */
    public List<CharacterizationStudy> list(Long institutionId) {
        return studies.findByInstitution(institutionId);
    }

    /**
     * Finds a study.
     *
     * @param id study id
     * @return the study
     * @throws ResourceNotFoundException if it does not exist (CA-207)
     */
    public CharacterizationStudy get(Long id) {
        return studies.findById(id).orElseThrow(() -> new ResourceNotFoundException("Estudio", id));
    }

    /**
     * Returns the sources taking part in a study.
     *
     * @param studyId study id
     * @return source ids
     */
    public Set<Long> participatingSources(Long studyId) {
        return studies.findParticipatingSources(studyId);
    }

    /**
     * Counts the records of a study still pending verification.
     *
     * @param studyId study id
     * @return pending records
     */
    public int pendingRecords(Long studyId) {
        return studies.countPendingRecords(studyId);
    }

    /**
     * Creates a planned study with its 8 dates. All the active sources of the
     * institution take part by default (R-331).
     *
     * @param actor         teacher that acts
     * @param institutionId institution to characterize
     * @param dates         the 8 dates in order; a date may be null while planning
     * @return the saved study
     */
    @Transactional
    public CharacterizationStudy create(User actor, Long institutionId, List<LocalDate> dates) {
        AccessPolicy.require(actor, Permission.MANAGE_STUDIES);
        StudyValidator.validateInstitutionAcceptsStudies(institutions.findById(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Institución", institutionId)));
        CharacterizationStudy study = new CharacterizationStudy(null, institutionId);
        study.scheduleDates(dates);
        StudyValidator.validate(study);
        StudyValidator.validateNoOverlap(study, studies.findByInstitution(institutionId));
        CharacterizationStudy saved = studies.save(study);
        Set<Long> activeSources = sources.findByInstitution(institutionId).stream()
                .filter(GenerationSource::isActive)
                .map(GenerationSource::getId)
                .collect(Collectors.toSet());
        studies.saveParticipatingSources(saved.getId(), activeSources);
        return saved;
    }

    /**
     * Changes the dates of the 8 days (R-330).
     *
     * @param actor teacher that acts
     * @param id    study id
     * @param dates the 8 dates in order
     * @return the saved study
     */
    @Transactional
    public CharacterizationStudy reschedule(User actor, Long id, List<LocalDate> dates) {
        AccessPolicy.require(actor, Permission.MANAGE_STUDIES);
        CharacterizationStudy study = editable(id);
        study.scheduleDates(dates);
        StudyValidator.validate(study);
        StudyValidator.validateNoOverlap(study, studies.findByInstitution(study.getInstitutionId()));
        return studies.save(study);
    }

    /**
     * Chooses the sources that take part in the study (R-331). They must belong
     * to the institution of the study.
     *
     * @param actor     teacher that acts
     * @param id        study id
     * @param sourceIds ids of the participating sources
     * @return the ids saved
     */
    @Transactional
    public Set<Long> setParticipatingSources(User actor, Long id, Set<Long> sourceIds) {
        AccessPolicy.require(actor, Permission.MANAGE_STUDIES);
        CharacterizationStudy study = editable(id);
        Map<Long, GenerationSource> ofInstitution = sources.findByInstitution(study.getInstitutionId()).stream()
                .collect(Collectors.toMap(GenerationSource::getId, Function.identity()));
        Set<Long> chosen = sourceIds == null ? Set.of() : Set.copyOf(sourceIds);
        for (Long sourceId : chosen) {
            if (!ofInstitution.containsKey(sourceId)) {
                throw new InvalidDataException("fuentes",
                        "La fuente " + sourceId + " no pertenece a la institución del estudio.");
            }
        }
        studies.saveParticipatingSources(id, chosen);
        return chosen;
    }

    /**
     * Moves the study to its next status, or reopens a closed one with a reason
     * (RN-014 to RN-017).
     *
     * @param actor  teacher that acts
     * @param id     study id
     * @param target status wanted
     * @param reason reason; required only to reopen a closed study
     * @return the saved study
     */
    @Transactional
    public CharacterizationStudy changeStatus(User actor, Long id, StudyStatus target, String reason) {
        AccessPolicy.require(actor, Permission.MANAGE_STUDIES);
        if (target == null) {
            throw new InvalidDataException("estado", "Debe indicar el nuevo estado del estudio.");
        }
        CharacterizationStudy study = get(id);
        if (study.getStatus() == StudyStatus.CERRADO && target == StudyStatus.EN_DEPURACION) {
            study.reopen(actor.getId(), reason, LocalDateTime.now(clock));
            return studies.save(study);
        }
        if (target == StudyStatus.EN_EJECUCION) {
            StudyValidator.validateStartExecution(study, activeParticipatingSources(study));
        } else if (target == StudyStatus.CERRADO) {
            StudyValidator.validateClose(study, studies.countPendingRecords(id), studies.countUnresolvedQuarterings(id));
        }
        study.changeStatus(target);
        return studies.save(study);
    }

    /**
     * Cancels a study being executed or cleaned, with a reason (RN-018). Its
     * data is kept and its dates become free for another study.
     *
     * @param actor  teacher that acts
     * @param id     study id
     * @param reason reason for the cancellation
     * @return the cancelled study
     */
    @Transactional
    public CharacterizationStudy cancel(User actor, Long id, String reason) {
        AccessPolicy.require(actor, Permission.MANAGE_STUDIES);
        CharacterizationStudy study = get(id);
        study.cancel(actor.getId(), reason, LocalDateTime.now(clock));
        return studies.save(study);
    }

    /**
     * Deletes a planned study with its days and participating sources (RN-019).
     *
     * @param actor teacher that acts
     * @param id    study id
     */
    @Transactional
    public void delete(User actor, Long id) {
        AccessPolicy.require(actor, Permission.MANAGE_STUDIES);
        get(id).requireDeletable();
        studies.delete(id);
    }

    private int activeParticipatingSources(CharacterizationStudy study) {
        Set<Long> participating = studies.findParticipatingSources(study.getId());
        return (int) sources.findByInstitution(study.getInstitutionId()).stream()
                .filter(source -> source.isActive() && participating.contains(source.getId()))
                .count();
    }

    private CharacterizationStudy editable(Long id) {
        CharacterizationStudy study = get(id);
        if (study.getStatus() == StudyStatus.CANCELADO) {
            throw new BusinessRuleException("El estudio está cancelado: no admite cambios.");
        }
        if (study.getStatus().isReadOnly()) {
            throw new BusinessRuleException("El estudio está cerrado: reábralo para hacer cambios.");
        }
        return study;
    }
}
