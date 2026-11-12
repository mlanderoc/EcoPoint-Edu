package com.ecopoint.dto;

import java.math.BigDecimal;

/**
 * Data of a bag (R-343). The source is read only when registering.
 *
 * @param sourceId    source the waste comes from
 * @param grossWeight gross weight, in kg
 * @param tare        tare, in kg
 * @param volume      volume, in m3
 * @author craulgomez
 * @version 1.0
 */
public record BagRequest(Long sourceId, BigDecimal grossWeight, BigDecimal tare, BigDecimal volume) {
}
