package com.ecopoint.service;

import com.ecopoint.model.Institution;
import com.ecopoint.model.User;
import com.ecopoint.persistence.UserActivity;

import java.util.List;

/**
 * Everything the "Mi Perfil" screen shows (spec 004, R-350 and R-351).
 *
 * @param user        user of the session
 * @param institution its institution, or null
 * @param crews       names of the crews the user leads or belongs to
 * @param activity    totals of the records of the user
 * @author EstefaniaMarquez
 * @version 1.0
 */
public record UserProfile(User user, Institution institution, List<String> crews, UserActivity activity) {
}
