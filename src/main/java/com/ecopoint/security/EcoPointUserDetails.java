package com.ecopoint.security;

import com.ecopoint.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Authenticated user kept in the session. It wraps the domain {@link User}; the
 * password is never kept in the session, because the login is checked before
 * (spec 003, decisions T1 and T2).
 *
 * @author mlanderoc
 * @version 1.0
 */
public class EcoPointUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final User user;

    /**
     * Creates the session user.
     *
     * @param user domain user already authenticated
     */
    public EcoPointUserDetails(User user) {
        this.user = user;
    }

    /**
     * Returns the domain user, used by the services to check permissions.
     *
     * @return the domain user
     */
    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
