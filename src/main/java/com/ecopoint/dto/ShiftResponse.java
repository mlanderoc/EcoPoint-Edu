package com.ecopoint.dto;

import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftStatus;
import com.ecopoint.model.ShiftType;
import com.ecopoint.service.SourceProgress;

import java.math.BigDecimal;
import java.util.List;

/**
 * Shift as the API returns it, with its population and the state of each
 * participating source (spec 003, section 5.2; R-345 and R-346).
 *
 * @param id                       shift id
 * @param studyId                  study of the shift
 * @param dayNumber                sampling day, 1 to 8
 * @param type                     morning or afternoon
 * @param code                     code, e.g. {@code D3-M}
 * @param status                   ABIERTO or CERRADO
 * @param crewId                   crew of the shift, or null
 * @param population               population, or null if not registered yet
 * @param bagCount                 number of bags
 * @param totalNetWeight           kg net of the shift (2 decimals)
 * @param sourcesWithoutGeneration sources marked without generation
 * @param hasQuartering            whether the quartering was registered
 * @param hasDensity               whether the density was measured
 * @param sources                  state of each participating source; empty in short answers
 * @author craulgomez
 * @version 1.0
 */
public record ShiftResponse(Long id, Long studyId, int dayNumber, ShiftType type, String code, ShiftStatus status,
                            Long crewId, PopulationDto population, int bagCount, BigDecimal totalNetWeight,
                            List<Long> sourcesWithoutGeneration, boolean hasQuartering, boolean hasDensity,
                            List<SourceProgressResponse> sources) {

    /**
     * Copies a shift of the domain with the progress of its sources.
     *
     * @param shift    domain shift
     * @param progress progress of each participating source
     * @return the response
     */
    public static ShiftResponse from(SamplingShift shift, List<SourceProgress> progress) {
        return new ShiftResponse(shift.getId(), shift.getStudyId(), shift.getDayNumber(), shift.getType(),
                shift.getCode(), shift.getStatus(), shift.getCrewId(), PopulationDto.from(shift.getPopulation()),
                shift.getBags().size(), Decimals.show(shift.calculateTotalNetWeight()),
                shift.getSourcesWithoutGeneration().stream().sorted().toList(), shift.hasQuartering(),
                shift.hasDensity(), progress.stream().map(SourceProgressResponse::from).toList());
    }
}
