package com.ecopoint.controller;

import com.ecopoint.persistence.UserActivity;
import com.ecopoint.dto.UserRowResponse;
import com.ecopoint.dto.PageResponse;
import com.ecopoint.dto.UserRequest;
import com.ecopoint.dto.UserResponse;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.security.EcoPointUserDetails;
import com.ecopoint.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Users (spec 003, section 5.1). Passwords are received but never returned.
 *
 * @author craulgomez
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController implements FieldNameMapping {

    private static final Map<String, String> FIELDS = Map.of(
            "nombre", "firstName", "apellido", "lastName", "correo", "email", "rol", "role",
            "contrasena", "password");

    private final UserService users;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param users       user service
     * @param currentUser user of the session
     */
    public UserController(UserService users, CurrentUser currentUser) {
        this.users = users;
        this.currentUser = currentUser;
    }

    @Override
    public Map<String, String> fieldNames() {
        return FIELDS;
    }

    /**
     * Lists the users of the institution, one page at a time (R-103).
     *
     * @param principal user of the session
     * @param role      role to keep, or none
     * @param active    state to keep, or none
     * @param q         text to look for in the name or the email, or none
     * @param page      page, starting at 0
     * @param size      page size
     * @return one page of users with their totals of records
     */
    @GetMapping
    public PageResponse<UserRowResponse> list(@AuthenticationPrincipal EcoPointUserDetails principal,
                                           @RequestParam(required = false) UserRole role,
                                           @RequestParam(required = false) Boolean active,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "50") int size) {
        User actor = currentUser.of(principal);
        Map<Long, UserActivity> activity = users.activities(actor);
        return PageResponse.of(users.list(actor, role, active, q), page, size,
                user -> UserRowResponse.from(user, activity.get(user.getId())));
    }

    /**
     * Shows one user.
     *
     * @param principal user of the session
     * @param id        user id
     * @return the user
     */
    @GetMapping("/{id}")
    public UserResponse get(@AuthenticationPrincipal EcoPointUserDetails principal, @PathVariable Long id) {
        return UserResponse.from(users.get(currentUser.of(principal), id), null);
    }

    /**
     * Creates a user with its initial password.
     *
     * @param principal user of the session
     * @param request   data of the user
     * @return the created user
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@AuthenticationPrincipal EcoPointUserDetails principal,
                               @RequestBody UserRequest request) {
        User created = users.create(currentUser.of(principal), request.toModel(), request.password());
        return UserResponse.from(created, null);
    }

    /**
     * Edits a user; the password changes only if one is sent.
     *
     * @param principal user of the session
     * @param id        user id
     * @param request   new data
     * @return the saved user
     */
    @PutMapping("/{id}")
    public UserResponse update(@AuthenticationPrincipal EcoPointUserDetails principal, @PathVariable Long id,
                               @RequestBody UserRequest request) {
        String newPassword = request.password() == null || request.password().isEmpty() ? null : request.password();
        return UserResponse.from(users.update(currentUser.of(principal), id, request.toModel(), newPassword), null);
    }

    /**
     * Deactivates a user.
     *
     * @param principal user of the session
     * @param id        user id
     * @return the user, now inactive
     */
    @PostMapping("/{id}/deactivate")
    public UserResponse deactivate(@AuthenticationPrincipal EcoPointUserDetails principal, @PathVariable Long id) {
        return UserResponse.from(users.deactivate(currentUser.of(principal), id), null);
    }

    /**
     * Activates a user again (RN-087).
     *
     * @param principal user of the session
     * @param id        user id
     * @return the user, now active
     */
    @PostMapping("/{id}/activate")
    public UserResponse activate(@AuthenticationPrincipal EcoPointUserDetails principal, @PathVariable Long id) {
        return UserResponse.from(users.activate(currentUser.of(principal), id), null);
    }
}
