package com.ecopoint.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Correction of a record after checking it against the paper form (RN-063).
 *
 * @param values new value of each corrected field, e.g. {@code {"grossWeight": 12.04}}
 * @param reason reason of the correction
 * @author craulgomez
 * @version 1.0
 */
public record CorrectionRequest(Map<String, BigDecimal> values, String reason) {
}
