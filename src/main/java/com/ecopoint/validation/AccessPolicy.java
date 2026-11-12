package com.ecopoint.validation;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.model.Permission;
import com.ecopoint.model.User;

/**
 * Stateless access policy that enforces the permission matrix (RN-081) on a
 * concrete user. Services call it before every protected action; the REST
 * layer translates its {@link AccessDeniedException} into HTTP 403.
 *
 * @author craulgomez
 * @version 1.0
 */
public final class AccessPolicy {

    private AccessPolicy() {
    }

    /**
     * Requires that the user exists, is active and holds the permission.
     *
     * @param user       user that wants to act
     * @param permission permission the action requires
     * @throws AccessDeniedException if the user is missing, inactive or lacks the permission
     */
    public static void require(User user, Permission permission) {
        if (user == null) {
            throw new AccessDeniedException("Debe identificarse para " + permission.getActionDescription() + ".");
        }
        if (!user.isActive()) {
            throw new AccessDeniedException("El usuario " + user.getFullName()
                    + " está inactivo y no puede " + permission.getActionDescription() + ".");
        }
        if (user.getRole() == null || !user.getRole().hasPermission(permission)) {
            String roleName = user.getRole() == null ? "sin asignar" : user.getRole().getDisplayName();
            throw new AccessDeniedException(roleName, permission.getActionDescription());
        }
    }

    /**
     * Indicates whether the user may perform the action, without throwing.
     * Useful to show or hide options in the user interface.
     *
     * @param user       user to check
     * @param permission permission the action requires
     * @return true if the user exists, is active and holds the permission
     */
    public static boolean can(User user, Permission permission) {
        return user != null && user.hasPermission(permission);
    }
}
