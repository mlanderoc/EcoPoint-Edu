package com.ecopoint.validation;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.model.Crew;
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
     * Requires that the user may register field data in a study: a teacher in
     * any study, a student only in the studies of a crew he belongs to (RN-089).
     *
     * @param user user that wants to register
     * @param crew crew of the study, or null if it has none
     * @throws AccessDeniedException if a student is not a member of the crew
     */
    public static void requireCrewMember(User user, Crew crew) {
        if (can(user, Permission.MANAGE_STUDIES)) {
            return;
        }
        if (crew == null || !crew.isMember(user)) {
            throw new AccessDeniedException("Solo los integrantes de la cuadrilla del estudio pueden registrar datos en él.");
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
