package com.ecopoint.service;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.Correction;
import com.ecopoint.model.Crew;
import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.Institution;
import com.ecopoint.model.Permission;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.StudyStatus;
import com.ecopoint.model.User;
import com.ecopoint.model.VerifiableRecord;
import com.ecopoint.persistence.CorrectionRepository;
import com.ecopoint.persistence.CrewRepository;
import com.ecopoint.persistence.GenerationSourceRepository;
import com.ecopoint.persistence.InstitutionRepository;
import com.ecopoint.persistence.ShiftRepository;
import com.ecopoint.persistence.StudyRepository;
import com.ecopoint.validation.AccessPolicy;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    private final FieldAccess fieldAccess;
    private final StudyRepository studies;
    private final ShiftRepository shifts;
    private final GenerationSourceRepository sources;
    private final InstitutionRepository institutions;
    private final CorrectionRepository corrections;
    private final CrewRepository crews;

    /**
     * Creates the service.
     *
     * @param studies      study repository
     * @param shifts       shift repository
     * @param sources      generation source repository
     * @param institutions institution repository
     * @param corrections  correction history repository
     * @param crews        crew repository
     * @param fieldAccess who may read the study (RN-097)
     */
    public IndicatorService(StudyRepository studies, ShiftRepository shifts, GenerationSourceRepository sources,
                            InstitutionRepository institutions, CorrectionRepository corrections,
                            CrewRepository crews,
                            FieldAccess fieldAccess) {
        this.fieldAccess = fieldAccess;
        this.studies = studies;
        this.shifts = shifts;
        this.sources = sources;
        this.institutions = institutions;
        this.corrections = corrections;
        this.crews = crews;
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
        fieldAccess.require(actor, study);
        return IndicatorCalculator.calculate(study, shifts.findByStudy(studyId), sourcesOf(study));
    }

    /**
     * Loads everything the Excel and PDF reports need (R-530, R-531, R-533).
     *
     * @param actor   user asking
     * @param studyId study id
     * @return the study, its shifts, its indicators, its correction history and its crews
     * @throws com.ecopoint.exceptions.AccessDeniedException if the role cannot view indicators
     * @throws ResourceNotFoundException                     if the study does not exist
     */
    public StudyReport report(User actor, Long studyId) {
        AccessPolicy.require(actor, Permission.VIEW_INDICATORS);
        CharacterizationStudy study = findStudy(studyId);
        fieldAccess.require(actor, study);
        List<SamplingShift> studyShifts = shifts.findByStudy(studyId);
        String institutionName = institutions.findById(study.getInstitutionId()).map(Institution::getName).orElse("");
        List<Correction> history = studyShifts.stream().flatMap(IndicatorService::recordsOf)
                .flatMap(record -> corrections.findByRecord(record.getRecordType(), record.getId()).stream())
                .sorted(Comparator.comparing(Correction::at))
                .toList();
        List<String> crewNames = studyShifts.stream().map(SamplingShift::getCrewId).filter(Objects::nonNull)
                .distinct().map(crews::findById).flatMap(Optional::stream).map(Crew::getName).sorted().toList();
        return new StudyReport(study, institutionName, studyShifts,
                IndicatorCalculator.calculate(study, studyShifts, sourcesOf(study)), history, crewNames);
    }

    /**
     * Finds the study of the indicators. A cancelled study has no baseline: it
     * is not shown in Indicators nor in the reports (RN-018, D13 task A20).
     */
    private CharacterizationStudy findStudy(Long studyId) {
        CharacterizationStudy study = studies.findById(studyId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudio", studyId));
        if (study.getStatus() == StudyStatus.CANCELADO) {
            throw new BusinessRuleException("El estudio está cancelado: no tiene indicadores ni reportes.");
        }
        return study;
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
