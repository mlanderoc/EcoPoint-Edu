package com.ecopoint.service;

import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.Correction;
import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.Institution;
import com.ecopoint.model.Permission;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.User;
import com.ecopoint.model.VerifiableRecord;
import com.ecopoint.persistence.CorrectionRepository;
import com.ecopoint.persistence.GenerationSourceRepository;
import com.ecopoint.persistence.InstitutionRepository;
import com.ecopoint.persistence.ShiftRepository;
import com.ecopoint.persistence.StudyRepository;
import com.ecopoint.validation.AccessPolicy;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final InstitutionRepository institutions;
    private final CorrectionRepository corrections;

    /**
     * Creates the service.
     *
     * @param studies      study repository
     * @param shifts       shift repository
     * @param sources      generation source repository
     * @param institutions institution repository
     * @param corrections  correction history repository
     */
    public IndicatorService(StudyRepository studies, ShiftRepository shifts, GenerationSourceRepository sources,
                            InstitutionRepository institutions, CorrectionRepository corrections) {
        this.studies = studies;
        this.shifts = shifts;
        this.sources = sources;
        this.institutions = institutions;
        this.corrections = corrections;
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
        CharacterizationStudy study = findStudy(studyId);
        return IndicatorCalculator.calculate(study, shifts.findByStudy(studyId), sourcesOf(study));
    }

    /**
     * Loads everything the Excel and PDF reports need (R-530, R-531, R-533).
     *
     * @param actor   user asking
     * @param studyId study id
     * @return the study, its shifts, its indicators and its correction history
     * @throws com.ecopoint.exceptions.AccessDeniedException if the role cannot view indicators
     * @throws ResourceNotFoundException                     if the study does not exist
     */
    public StudyReport report(User actor, Long studyId) {
        AccessPolicy.require(actor, Permission.VIEW_INDICATORS);
        CharacterizationStudy study = findStudy(studyId);
        List<SamplingShift> studyShifts = shifts.findByStudy(studyId);
        String institutionName = institutions.findById(study.getInstitutionId()).map(Institution::getName).orElse("");
        List<Correction> history = studyShifts.stream().flatMap(IndicatorService::recordsOf)
                .flatMap(record -> corrections.findByRecord(record.getRecordType(), record.getId()).stream())
                .sorted(Comparator.comparing(Correction::at))
                .toList();
        return new StudyReport(study, institutionName, studyShifts,
                IndicatorCalculator.calculate(study, studyShifts, sourcesOf(study)), history);
    }

    private CharacterizationStudy findStudy(Long studyId) {
        return studies.findById(studyId).orElseThrow(() -> new ResourceNotFoundException("Estudio", studyId));
    }

    private Map<Long, GenerationSource> sourcesOf(CharacterizationStudy study) {
        return sources.findByInstitution(study.getInstitutionId()).stream()
                .collect(Collectors.toMap(GenerationSource::getId, Function.identity()));
    }

    private static Stream<VerifiableRecord> recordsOf(SamplingShift shift) {
        return Stream.of(Stream.<VerifiableRecord>ofNullable(shift.getPopulation()),
                        shift.getBags().stream().map(VerifiableRecord.class::cast),
                        Stream.<VerifiableRecord>ofNullable(shift.getQuartering()),
                        Stream.<VerifiableRecord>ofNullable(shift.getDensity()))
                .flatMap(Function.identity());
    }
}
