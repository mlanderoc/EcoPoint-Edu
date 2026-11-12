package com.ecopoint.dto;

import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.SourceType;

/**
 * Generation source as the API returns it. The polygon is served by the map of
 * spec 007; here only whether it has one.
 *
 * @param id            source id
 * @param institutionId institution of the source
 * @param code          code
 * @param name          name
 * @param type          type
 * @param active        whether it accepts new records (RN-005)
 * @param hasGeometry   whether it already has a polygon on the map
 * @author craulgomez
 * @version 1.0
 */
public record SourceResponse(Long id, Long institutionId, String code, String name, SourceType type, boolean active,
                             boolean hasGeometry) {

    /**
     * Copies a source of the domain.
     *
     * @param source domain source
     * @return the response
     */
    public static SourceResponse from(GenerationSource source) {
        return new SourceResponse(source.getId(), source.getInstitutionId(), source.getCode(), source.getName(),
                source.getType(), source.isActive(), source.hasGeometry());
    }
}
