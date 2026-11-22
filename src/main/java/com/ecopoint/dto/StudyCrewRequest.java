package com.ecopoint.dto;

/**
 * Request to change the crew of a planned study (RN-088).
 *
 * @param crewId new crew
 * @author craulgomez
 * @version 1.0
 */
public record StudyCrewRequest(Long crewId) {
}
