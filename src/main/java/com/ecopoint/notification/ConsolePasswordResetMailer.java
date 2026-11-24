package com.ecopoint.notification;

import com.ecopoint.model.User;
import com.ecopoint.service.PasswordResetMailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes the link in the backend console instead of sending an email, to try
 * the recovery on a local computer (spec D18, RP-11). It is never used with the
 * {@code prod} profile.
 *
 * @author mlanderoc
 * @version 1.0
 */
public class ConsolePasswordResetMailer implements PasswordResetMailer {

    private static final Logger LOG = LoggerFactory.getLogger(ConsolePasswordResetMailer.class);

    private final String frontendOrigin;

    /**
     * Creates the mailer.
     *
     * @param frontendOrigin address of the frontend, used in the link
     */
    public ConsolePasswordResetMailer(String frontendOrigin) {
        this.frontendOrigin = frontendOrigin;
    }

    @Override
    public void sendResetLink(User user, String code) {
        LOG.warn("Correo de recuperación sin enviar (solo local). Enlace para {}: {}", user.getEmail(),
                ResetEmail.link(frontendOrigin, code));
    }
}
