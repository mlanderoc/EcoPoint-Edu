package com.ecopoint.dto;

import java.util.Set;

/**
 * Sources that take part in a study (R-331).
 *
 * @param sourceIds ids of the participating sources
 * @author craulgomez
 * @version 1.0
 */
public record SourceIdsRequest(Set<Long> sourceIds) {
}
