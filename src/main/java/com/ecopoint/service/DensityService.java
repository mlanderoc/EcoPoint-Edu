package com.ecopoint.service;

import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.DensityMeasurement;
import com.ecopoint.model.Permission;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.User;
import com.ecopoint.model.VerificationInfo;
import com.ecopoint.persistence.DensityRepository;
import com.ecopoint.persistence.ShiftRepository;
import com.ecopoint.persistence.StudyRepository;
import com.ecopoint.validation.AccessPolicy;
import com.ecopoint.validation.DensityValidator;
import com.ecopoint.validation.ShiftValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Density measurement of a shift (RN-057; spec 003, section 5.3). It is
 * registered with the quartering permission, as RN-081 groups them.
 *
 * @author craulgomez
 * @version 1.0
 */
@Service
public class DensityService {

    private final DensityRepository densities;
    private final ShiftRepository shifts;
    private final StudyRepository studies;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param densities density repository
     * @param shifts    shift repository
     * @param studies   study repository
     * @param clock     clock that dates the records
     */
    public DensityService(DensityRepository densities, ShiftRepository shifts, StudyRepository studies, Clock clock) {
        this.densities = densities;
        this.shifts = shifts;
        this.studies = studies;
        this.clock = clock;
    }

    /**
     * Finds the density measurement of a shift.
     *
     * @param shiftId shift id
     * @return the measurement
     * @throws ResourceNotFoundException if the shift has no measurement
     */
    public DensityMeasurement get(Long shiftId) {
        return densities.findByShift(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Medición de densidad del turno", shiftId));
    }

    /**
     * Registers the density measurement of a shift; a shift has at most one (RN-057).
     *
     * @param actor           user that types it
     * @param shiftId         shift id
     * @param containerVolume volume of the measuring container, in m³
     * @param netWeight       net weight of the waste in the container, in kg
     * @return the saved measurement with its density
     */
    @Transactional
    public DensityMeasurement register(User actor, Long shiftId, BigDecimal containerVolume, BigDecimal netWeight) {
        AccessPolicy.require(actor, Permission.REGISTER_QUARTERING);
        SamplingShift shift = shifts.findById(shiftId).orElseThrow(() -> new ResourceNotFoundException("Turno", shiftId));
        CharacterizationStudy study = studies.findById(shift.getStudyId())
                .orElseThrow(() -> new ResourceNotFoundException("Estudio", shift.getStudyId()));
        ShiftValidator.validateOpenForRegistration(shift, study);
        DensityValidator.validateNoExistingDensity(shift);
        DensityMeasurement measurement = new DensityMeasurement(null, shiftId, containerVolume, netWeight,
                new VerificationInfo(actor.getId(), LocalDateTime.now(clock)));
        DensityValidator.validate(measurement);
        shift.registerDensity(measurement);
        return densities.save(measurement);
    }
}
