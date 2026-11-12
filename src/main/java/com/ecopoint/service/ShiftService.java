package com.ecopoint.service;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.Permission;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftPopulation;
import com.ecopoint.model.ShiftType;
import com.ecopoint.model.User;
import com.ecopoint.model.VerificationInfo;
import com.ecopoint.model.WasteBag;
import com.ecopoint.persistence.CrewRepository;
import com.ecopoint.persistence.GenerationSourceRepository;
import com.ecopoint.persistence.ShiftRepository;
import com.ecopoint.persistence.StudyRepository;
import com.ecopoint.validation.AccessPolicy;
import com.ecopoint.validation.ShiftValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sampling shifts: open, close and reopen, population and "no generation"
 * marks (RN-020 to RN-025; spec 003, section 5.2; spec 004, R-340 to R-346).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Service
public class ShiftService {

    private final ShiftRepository shifts;
    private final StudyRepository studies;
    private final GenerationSourceRepository sources;
    private final CrewRepository crews;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param shifts  shift repository
     * @param studies study repository
     * @param sources source repository
     * @param crews   crew repository
     * @param clock   clock that dates the records
     */
    public ShiftService(ShiftRepository shifts, StudyRepository studies, GenerationSourceRepository sources,
                        CrewRepository crews, Clock clock) {
        this.shifts = shifts;
        this.studies = studies;
        this.sources = sources;
        this.crews = crews;
        this.clock = clock;
    }

    /**
     * Lists the 16 possible shifts of a study, opened or not, with their progress.
     *
     * @param studyId study id
     * @return one slot per day and shift type, in order
     */
    public List<ShiftSlot> slots(Long studyId) {
        CharacterizationStudy study = study(studyId);
        Map<Long, GenerationSource> participating = participatingSources(study);
        List<SamplingShift> opened = shifts.findByStudy(studyId);
        List<ShiftSlot> slots = new ArrayList<>();
        for (int day = 1; day <= CharacterizationStudy.SAMPLING_DAYS; day++) {
            for (ShiftType type : ShiftType.values()) {
                int dayNumber = day;
                SamplingShift shift = opened.stream()
                        .filter(found -> found.getDayNumber() == dayNumber && found.getType() == type)
                        .findFirst().orElse(null);
                int covered = shift == null ? 0
                        : (int) participating.keySet().stream().filter(shift::isSourceCovered).count();
                slots.add(new ShiftSlot(day, type, shift, covered, participating.size()));
            }
        }
        return slots;
    }

    /**
     * Finds a shift with everything recorded in it.
     *
     * @param id shift id
     * @return the shift
     * @throws ResourceNotFoundException if it does not exist
     */
    public SamplingShift get(Long id) {
        return shifts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Turno", id));
    }

    /**
     * Summarizes, for each participating source, its bags and its state in the shift (R-345).
     *
     * @param shift shift to summarize
     * @return one entry per participating source, ordered by code
     */
    public List<SourceProgress> progress(SamplingShift shift) {
        return participatingSources(study(shift.getStudyId())).values().stream()
                .map(source -> progressOf(shift, source))
                .toList();
    }

    /**
     * Opens a shift of a study in execution (RN-020, R-341).
     *
     * @param actor     teacher that opens it
     * @param studyId   study id
     * @param dayNumber sampling day, 1 to 8
     * @param type      morning or afternoon
     * @param crewId    crew that works the shift, or null
     * @return the opened shift
     */
    @Transactional
    public SamplingShift open(User actor, Long studyId, int dayNumber, ShiftType type, Long crewId) {
        AccessPolicy.require(actor, Permission.MANAGE_SHIFTS);
        CharacterizationStudy study = study(studyId);
        ShiftValidator.validateNewShift(study, dayNumber, type, shifts.findByStudy(studyId));
        if (crewId != null && crews.findById(crewId).isEmpty()) {
            throw new ResourceNotFoundException("Cuadrilla", crewId);
        }
        SamplingShift shift = new SamplingShift(null, studyId, dayNumber, type);
        shift.assignCrew(crewId);
        return shifts.save(shift);
    }

    /**
     * Closes a shift when every participating source is accounted for (RN-024, R-346).
     *
     * @param actor   teacher that closes it
     * @param shiftId shift id
     * @return the closed shift
     */
    @Transactional
    public SamplingShift close(User actor, Long shiftId) {
        AccessPolicy.require(actor, Permission.MANAGE_SHIFTS);
        SamplingShift shift = get(shiftId);
        Map<Long, String> codes = new LinkedHashMap<>();
        participatingSources(study(shift.getStudyId())).forEach((id, source) -> codes.put(id, source.getCode()));
        ShiftValidator.validateCanClose(shift, codes);
        shift.close();
        return shifts.save(shift);
    }

    /**
     * Reopens a closed shift while the study is in execution (RN-025).
     *
     * @param actor   teacher that reopens it
     * @param shiftId shift id
     * @return the reopened shift
     */
    @Transactional
    public SamplingShift reopen(User actor, Long shiftId) {
        AccessPolicy.require(actor, Permission.MANAGE_SHIFTS);
        SamplingShift shift = get(shiftId);
        ShiftValidator.validateReopen(shift, study(shift.getStudyId()));
        shift.reopen();
        return shifts.save(shift);
    }

    /**
     * Registers the people present in the shift (RN-022, R-342).
     *
     * @param actor    user that types it
     * @param shiftId  shift id
     * @param students students present
     * @param teachers teachers present
     * @param staff    staff present
     * @return the shift with its population
     */
    @Transactional
    public SamplingShift registerPopulation(User actor, Long shiftId, int students, int teachers, int staff) {
        AccessPolicy.require(actor, Permission.REGISTER_POPULATION);
        SamplingShift shift = get(shiftId);
        ShiftValidator.validateOpenForRegistration(shift, study(shift.getStudyId()));
        ShiftPopulation population = new ShiftPopulation(null, students, teachers, staff,
                new VerificationInfo(actor.getId(), LocalDateTime.now(clock)));
        ShiftValidator.validatePopulation(population);
        shift.registerPopulation(population);
        return shifts.save(shift);
    }

    /**
     * Marks or unmarks a participating source as having generated no waste in
     * the shift (RN-024).
     *
     * @param actor        user that marks it
     * @param shiftId      shift id
     * @param sourceId     source id
     * @param noGeneration true to mark, false to remove the mark
     * @return the shift with its marks
     */
    @Transactional
    public SamplingShift setNoGeneration(User actor, Long shiftId, Long sourceId, boolean noGeneration) {
        AccessPolicy.require(actor, Permission.REGISTER_BAGS);
        SamplingShift shift = get(shiftId);
        CharacterizationStudy study = study(shift.getStudyId());
        ShiftValidator.validateOpenForRegistration(shift, study);
        if (!studies.findParticipatingSources(study.getId()).contains(sourceId)) {
            throw new BusinessRuleException("La fuente " + sourceId + " no participa en este estudio.");
        }
        if (noGeneration) {
            shift.markSourceWithoutGeneration(sourceId);
        } else {
            shift.unmarkSourceWithoutGeneration(sourceId);
        }
        return shifts.save(shift);
    }

    private SourceProgress progressOf(SamplingShift shift, GenerationSource source) {
        List<WasteBag> bags = shift.getBagsFrom(source.getId());
        BigDecimal net = bags.stream().map(WasteBag::getNetWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
        SourceShiftStatus status = !bags.isEmpty() ? SourceShiftStatus.CON_GENERACION
                : shift.getSourcesWithoutGeneration().contains(source.getId()) ? SourceShiftStatus.SIN_GENERACION
                : SourceShiftStatus.PENDIENTE;
        return new SourceProgress(source, bags.size(), net, status);
    }

    private Map<Long, GenerationSource> participatingSources(CharacterizationStudy study) {
        Set<Long> ids = studies.findParticipatingSources(study.getId());
        Map<Long, GenerationSource> participating = new LinkedHashMap<>();
        sources.findByInstitution(study.getInstitutionId()).stream()
                .filter(source -> ids.contains(source.getId()))
                .forEach(source -> participating.put(source.getId(), source));
        return participating;
    }

    private CharacterizationStudy study(Long studyId) {
        return studies.findById(studyId).orElseThrow(() -> new ResourceNotFoundException("Estudio", studyId));
    }
}
