package com.ecopoint.dto;

import com.ecopoint.model.SourceType;

/**
 * Data to create or edit a generation source (spec 004, R-321).
 *
 * @param code code, 2 to 6 capital letters or digits (RN-002)
 * @param name name
 * @param type   type of source (RN-003)
 * @param active false to create it inactive (RN-005); ignored when editing
 * @author craulgomez
 * @version 1.0
 */
public record SourceRequest(String code, String name, SourceType type, Boolean active) {

    /**
     * Indicates whether the source is created active: it is unless the form
     * says otherwise (R-321).
     *
     * @return false only when {@code active} is false
     */
    public boolean createActive() {
        return !Boolean.FALSE.equals(active);
    }
}
