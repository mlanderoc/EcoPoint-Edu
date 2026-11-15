package com.ecopoint.dto;

import com.ecopoint.service.UserProfile;

import java.util.List;

/**
 * Data of the "Mi Perfil" screen (spec 004, R-350 and R-351).
 *
 * @param user     user of the session with his institution and permissions
 * @param crews    names of his crews
 * @param activity his own totals of records
 * @author craulgomez
 * @version 1.0
 */
public record ProfileResponse(UserResponse user, List<String> crews, UserActivityResponse activity) {

    /**
     * Copies the profile of the service.
     *
     * @param profile profile of the user
     * @return the response
     */
    public static ProfileResponse from(UserProfile profile) {
        return new ProfileResponse(UserResponse.from(profile.user(), profile.institution()), profile.crews(),
                UserActivityResponse.from(profile.activity()));
    }
}
