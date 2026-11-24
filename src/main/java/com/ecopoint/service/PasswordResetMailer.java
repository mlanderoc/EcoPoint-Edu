package com.ecopoint.service;

import com.ecopoint.model.User;

/**
 * Sends the email with the link to create a new password (spec D18, RP-09).
 * The service depends on this interface, never on a mail provider (decision P4).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public interface PasswordResetMailer {

    /**
     * Sends the link. It must not block the answer to the user (decision P3)
     * and must not throw if the provider fails: the failure is only logged.
     *
     * @param user recipient
     * @param code single-use code that goes in the link, never stored
     */
    void sendResetLink(User user, String code);
}
