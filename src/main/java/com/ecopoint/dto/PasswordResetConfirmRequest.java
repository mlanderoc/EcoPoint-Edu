package com.ecopoint.dto;

/**
 * New password sent with the code of the recovery link (spec D18, RP-06).
 *
 * @param token       code of the link
 * @param newPassword new password
 * @author craulgomez
 * @version 1.0
 */
public record PasswordResetConfirmRequest(String token, String newPassword) {
}
