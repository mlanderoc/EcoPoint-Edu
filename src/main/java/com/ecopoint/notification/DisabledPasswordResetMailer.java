package com.ecopoint.notification;

import com.ecopoint.model.User;
import com.ecopoint.service.PasswordResetMailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used in production when the email service is not configured: it sends
 * nothing and logs the error without the link (spec D18, RP-11).
 *
 * @author mlanderoc
 * @version 1.0
 */
public class DisabledPasswordResetMailer implements PasswordResetMailer {

    private static final Logger LOG = LoggerFactory.getLogger(DisabledPasswordResetMailer.class);

    @Override
    public void sendResetLink(User user, String code) {
        LOG.error("El servicio de correo no está configurado: no se envió el enlace de recuperación del usuario {}.",
                user.getId());
    }
}
