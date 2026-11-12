package com.ecopoint.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * User roles of a characterization study (RN-080) and the single source of
 * truth for the permission matrix (RN-081). The domain, the REST API and the
 * web screens all ask this enum what a role may do.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public enum UserRole {

    DOCENTE("Docente", true, EnumSet.allOf(Permission.class)),

    ESTUDIANTE_UNIVERSITARIO("Estudiante universitario", false, EnumSet.of(
            Permission.REGISTER_POPULATION,
            Permission.REGISTER_BAGS,
            Permission.REGISTER_QUARTERING,
            Permission.REGISTER_HAZARDOUS_WASTE,
            Permission.VERIFY_RECORDS,
            Permission.ANNUL_QUARTERING,
            Permission.ATTEND_ALERTS,
            Permission.VIEW_INDICATORS));

    private final String displayName;
    private final boolean supervisor;
    private final Set<Permission> permissions;

    UserRole(String displayName, boolean supervisor, EnumSet<Permission> permissions) {
        this.displayName = displayName;
        this.supervisor = supervisor;
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    /**
     * Returns the role name shown to the user, in Spanish.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Indicates whether the role holds the given permission (RN-081).
     *
     * @param permission permission to check
     * @return true if the role may perform the action
     */
    public boolean hasPermission(Permission permission) {
        return permission != null && permissions.contains(permission);
    }

    /**
     * Returns all the permissions of the role.
     *
     * @return unmodifiable set of permissions
     */
    public Set<Permission> getPermissions() {
        return permissions;
    }

    /**
     * Indicates whether a user with this role may be the person in charge of
     * a crew (RN-085).
     *
     * @return true for DOCENTE
     */
    public boolean canSuperviseCrew() {
        return supervisor;
    }
}
