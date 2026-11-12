package com.ecopoint.dto;

import java.util.List;

/**
 * Data to create or edit a crew (spec 004, R-323).
 *
 * @param name          crew name
 * @param responsibleId id of the responsible teacher (RN-085)
 * @param memberIds     ids of the members
 * @author craulgomez
 * @version 1.0
 */
public record CrewRequest(String name, Long responsibleId, List<Long> memberIds) {
}
