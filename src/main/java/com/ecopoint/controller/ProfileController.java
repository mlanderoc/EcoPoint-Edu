package com.ecopoint.controller;

import com.ecopoint.dto.PasswordChangeRequest;
import com.ecopoint.dto.ProfileResponse;
import com.ecopoint.security.EcoPointUserDetails;
import com.ecopoint.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Profile of the user of the session (spec 003, R-132 and R-133; spec 004,
 * R-350 to R-352). Every user sees and changes only his own profile.
 *
 * @author craulgomez
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/auth/me")
public class ProfileController implements FieldNameMapping {

    private static final Map<String, String> FIELDS = Map.of(
            "contrasenaActual", "currentPassword", "contrasena", "newPassword");

    private final ProfileService profiles;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param profiles    profile service
     * @param currentUser user of the session
     */
    public ProfileController(ProfileService profiles, CurrentUser currentUser) {
        this.profiles = profiles;
        this.currentUser = currentUser;
    }

    @Override
    public Map<String, String> fieldNames() {
        return FIELDS;
    }

    /**
     * Returns the profile of the user of the session: data, crews and his own
     * totals of records.
     *
     * @param principal user of the session
     * @return the profile
     */
    @GetMapping("/profile")
    public ProfileResponse profile(@AuthenticationPrincipal EcoPointUserDetails principal) {
        return ProfileResponse.from(profiles.profile(currentUser.of(principal)));
    }

    /**
     * Changes the password of the user of the session (R-132).
     *
     * @param principal user of the session
     * @param request   current and new password
     */
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal EcoPointUserDetails principal,
                               @RequestBody PasswordChangeRequest request) {
        profiles.changePassword(currentUser.of(principal), request.currentPassword(), request.newPassword());
    }
}
