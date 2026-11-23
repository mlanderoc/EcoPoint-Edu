package com.ecopoint.dto;

/**
 * Crew that cannot take a study because of another open study (RN-098).
 *
 * @param crewId  crew id
 * @param studyId id of the open study that holds it
 * @author craulgomez
 * @version 1.0
 */
public record UnavailableCrewResponse(Long crewId, Long studyId) {
}
