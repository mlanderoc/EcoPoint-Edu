package com.ecopoint.dto;

/**
 * Change of the own password (spec 003, R-132).
 *
 * @param currentPassword password used now
 * @param newPassword     new password
 * @author craulgomez
 * @version 1.0
 */
public record PasswordChangeRequest(String currentPassword, String newPassword) {
}
