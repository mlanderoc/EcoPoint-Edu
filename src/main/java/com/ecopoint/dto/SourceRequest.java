package com.ecopoint.dto;

import com.ecopoint.model.SourceType;

/**
 * Data to create or edit a generation source (spec 004, R-321).
 *
 * @param code code, 2 to 6 capital letters or digits (RN-002)
 * @param name name
 * @param type type of source (RN-003)
 * @author craulgomez
 * @version 1.0
 */
public record SourceRequest(String code, String name, SourceType type) {
}
