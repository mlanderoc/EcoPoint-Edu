package com.ecopoint.dto;

/**
 * Request of a link to recover the password (spec D18, RP-01).
 *
 * @param email email of the account
 * @author craulgomez
 * @version 1.0
 */
public record PasswordResetRequest(String email) {
}
