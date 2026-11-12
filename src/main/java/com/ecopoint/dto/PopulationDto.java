package com.ecopoint.dto;

import com.ecopoint.model.ShiftPopulation;

/**
 * People present in a shift (RN-022, R-342). On requests only the three
 * counts are read; the total and the verification are calculated.
 *
 * @param id           population id, or null on requests
 * @param students     students present
 * @param teachers     teachers present
 * @param staff        staff present
 * @param total        sum of the three counts
 * @param verification double-entry data, or null on requests
 * @author craulgomez
 * @version 1.0
 */
public record PopulationDto(Long id, int students, int teachers, int staff, int total, VerificationDto verification) {

    /**
     * Copies a population of the domain.
     *
     * @param population domain population, or null
     * @return the DTO, or null
     */
    public static PopulationDto from(ShiftPopulation population) {
        if (population == null) {
            return null;
        }
        return new PopulationDto(population.getId(), population.getStudents(), population.getTeachers(),
                population.getStaff(), population.getTotal(), VerificationDto.from(population.getVerification()));
    }
}
