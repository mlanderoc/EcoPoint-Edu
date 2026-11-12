package com.ecopoint.security;

import com.ecopoint.model.User;
import com.ecopoint.persistence.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads a user and its password hash by email for Spring Security (spec 003, R-120).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Service
public class EcoPointUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    /**
     * Creates the service.
     *
     * @param users user repository
     */
    public EcoPointUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = users.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));
        String hash = users.findPasswordHashByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));
        return new EcoPointUserDetails(user, hash);
    }
}
