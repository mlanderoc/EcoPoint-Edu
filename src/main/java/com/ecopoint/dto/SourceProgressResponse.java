package com.ecopoint.dto;

import com.ecopoint.model.SourceType;
import com.ecopoint.service.SourceProgress;
import com.ecopoint.service.SourceShiftStatus;

import java.math.BigDecimal;

/**
 * State of a participating source in a shift (R-345).
 *
 * @param sourceId       source id
 * @param code           source code
 * @param name           source name
 * @param type           source type
 * @param bagCount       bags of the source in the shift
 * @param totalNetWeight kg net of those bags (2 decimals)
 * @param status         CON_GENERACION, SIN_GENERACION or PENDIENTE
 * @author craulgomez
 * @version 1.0
 */
public record SourceProgressResponse(Long sourceId, String code, String name, SourceType type, int bagCount,
                                     BigDecimal totalNetWeight, SourceShiftStatus status) {

    /**
     * Copies the progress of a source.
     *
     * @param progress progress calculated by the shift service
     * @return the response
     */
    public static SourceProgressResponse from(SourceProgress progress) {
        return new SourceProgressResponse(progress.source().getId(), progress.source().getCode(),
                progress.source().getName(), progress.source().getType(), progress.bagCount(),
                Decimals.show(progress.totalNetWeight()), progress.status());
    }
}
