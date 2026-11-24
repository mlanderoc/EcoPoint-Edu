package com.ecopoint.security;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

/**
 * Keeps track of the open sessions so that changing the password closes the
 * other ones (R-352).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Component
public class SessionControl {

    private final SessionRegistry registry;

    /**
     * Creates the component.
     *
     * @param registry registry of open sessions
     */
    public SessionControl(SessionRegistry registry) {
        this.registry = registry;
    }

    /**
     * Registers a session that has just been opened.
     *
     * @param sessionId id of the session
     * @param principal user of the session
     */
    public void register(String sessionId, EcoPointUserDetails principal) {
        registry.registerNewSession(sessionId, principal);
    }

    /**
     * Expires every session of the user except the current one.
     *
     * @param principal        user of the session
     * @param currentSessionId session that stays open
     */
    public void closeOtherSessions(EcoPointUserDetails principal, String currentSessionId) {
        registry.getAllSessions(principal, false).stream()
                .filter(session -> !session.getSessionId().equals(currentSessionId))
                .forEach(SessionInformation::expireNow);
    }
}
