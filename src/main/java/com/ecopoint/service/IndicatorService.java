package com.ecopoint.service;

import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.Permission;
import com.ecopoint.model.User;
import com.ecopoint.persistence.GenerationSourceRepository;
import com.ecopoint.persistence.ShiftRepository;
import com.ecopoint.persistence.StudyRepository;
import com.ecopoint.validation.AccessPolicy;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Baseline indicators of a study (spec 006). It loads the current values of
 * the records (R-501) and delegates the formulas to {@link IndicatorCalculator},
 * so the dashboard and the reports use the same numbers (R-533).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Service
public class IndicatorService {

    private final StudyRepository studies;
    private final ShiftRepository shifts;
    private final GenerationSourceRepository sources;

    /**
     * Creates the service.
     *
     * @param studies study repository
     * @param shifts  shift repository
     * @param sources generation source repository
     */
    public IndicatorService(StudyRepository studies, ShiftRepository shifts, GenerationSourceRepository sources) {
        this.studies = studies;
        this.shifts = shifts;
        this.sources = sources;
    }

    /**
     * Calculates the indicators of a study.
     *
     * @param actor   user asking
     * @param studyId study id
     * @return the indicators with full precision
     * @throws com.ecopoint.exceptions.AccessDeniedException if the role cannot view indicators
     * @throws ResourceNotFoundException                     if the study does not exist
     */
    public StudyIndicators calculate(User actor, Long studyId) {
        AccessPolicy.require(actor, Permission.VIEW_INDICATORS);
        CharacterizationStudy study = studies.findById(studyId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudio", studyId));
        Map<Long, GenerationSource> sourcesById = sources.findByInstitution(study.getInstitutionId()).stream()
                .collect(Collectors.toMap(GenerationSource::getId, Function.identity()));
        return IndicatorCalculator.calculate(study, shifts.findByStudy(studyId), sourcesById);
    }
}
