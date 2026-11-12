package com.ecopoint.dto;

/**
 * A written reason or note: to annul a quartering (RN-052) or to attend an
 * alert (RN-073).
 *
 * @param reason the text written by the user
 * @author craulgomez
 * @version 1.0
 */
public record ReasonRequest(String reason) {
}
