package com.ecopoint.dto;

import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.persistence.UserActivity;

/**
 * One row of the users table (spec 004, R-322).
 *
 * @param id        user id
 * @param firstName first name
 * @param lastName  last name
 * @param email     email
 * @param role      role (RN-080)
 * @param active    whether the user can log in
 * @param activity  totals of his records
 * @author craulgomez
 * @version 1.0
 */
public record UserRowResponse(Long id, String firstName, String lastName, String email, UserRole role,
                              boolean active, UserActivityResponse activity) {

    /**
     * Copies a user with his totals.
     *
     * @param user     domain user
     * @param activity his totals, or null if he has no records
     * @return the row
     */
    public static UserRowResponse from(User user, UserActivity activity) {
        return new UserRowResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getRole(), user.isActive(),
                UserActivityResponse.from(activity == null ? UserActivity.none(user.getId()) : activity));
    }
}
