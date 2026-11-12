package com.ecopoint.dto;

import com.ecopoint.model.Institution;
import com.ecopoint.model.Permission;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;

import java.util.Comparator;
import java.util.List;

/**
 * User as the API returns it: never with its password (spec 003, R-102 and R-125).
 *
 * @param id              user id
 * @param firstName       first name
 * @param lastName        last name
 * @param email           email
 * @param role            role (RN-080)
 * @param active          whether the user can log in
 * @param institutionId   institution of the user
 * @param institutionName name of the institution, or null when not loaded
 * @param permissions     permissions of the role (RN-081), so the menu does not repeat the matrix
 * @author craulgomez
 * @version 1.0
 */
public record UserResponse(Long id, String firstName, String lastName, String email, UserRole role, boolean active,
                           Long institutionId, String institutionName, List<Permission> permissions) {

    /**
     * Copies a user of the domain.
     *
     * @param user        domain user
     * @param institution its institution, or null
     * @return the response
     */
    public static UserResponse from(User user, Institution institution) {
        List<Permission> permissions = user.getRole().getPermissions().stream()
                .sorted(Comparator.comparing(Enum::ordinal))
                .toList();
        return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getRole(), user.isActive(), user.getInstitutionId(),
                institution == null ? null : institution.getName(), permissions);
    }
}
