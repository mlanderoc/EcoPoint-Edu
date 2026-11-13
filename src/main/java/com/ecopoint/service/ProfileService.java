package com.ecopoint.service;

import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.Crew;
import com.ecopoint.model.User;
import com.ecopoint.persistence.CrewRepository;
import com.ecopoint.persistence.InstitutionRepository;
import com.ecopoint.persistence.UserActivityRepository;
import com.ecopoint.persistence.UserRepository;
import com.ecopoint.validation.UserValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Profile of the user of the session: his data, his crews, his own totals of
 * records and the change of his password (spec 003, R-132 and R-133; spec 004,
 * R-350 to R-352). Every user only sees and changes his own profile, so no
 * permission of the matrix is needed.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Service
public class ProfileService {

    private final UserRepository users;
    private final InstitutionRepository institutions;
    private final CrewRepository crews;
    private final UserActivityRepository activity;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates the service.
     *
     * @param users           user repository
     * @param institutions    institution repository
     * @param crews           crew repository
     * @param activity        totals of records by user
     * @param passwordEncoder password hashing
     */
    public ProfileService(UserRepository users, InstitutionRepository institutions, CrewRepository crews,
                          UserActivityRepository activity, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.institutions = institutions;
        this.crews = crews;
        this.activity = activity;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Returns the profile of the user of the session.
     *
     * @param actor user of the session
     * @return his data, institution, crews and totals of records
     */
    public UserProfile profile(User actor) {
        List<String> crewNames = crews.findByInstitution(actor.getInstitutionId()).stream()
                .filter(crew -> leads(crew, actor) || crew.isMember(actor))
                .map(Crew::getName)
                .toList();
        return new UserProfile(actor, institutions.findById(actor.getInstitutionId()).orElse(null), crewNames,
                activity.findByUser(actor.getId()));
    }

    /**
     * Changes the password of the user of the session, after checking the
     * current one (R-132).
     *
     * @param actor           user of the session
     * @param currentPassword password he uses now
     * @param newPassword     new password, with the same rules as when a user is created
     * @throws InvalidDataException if the current password is wrong or the new one is invalid
     */
    @Transactional
    public void changePassword(User actor, String currentPassword, String newPassword) {
        String hash = users.findPasswordHashByEmail(actor.getEmail()).orElse("");
        if (currentPassword == null || hash.isEmpty() || !passwordEncoder.matches(currentPassword, hash)) {
            throw new InvalidDataException("contrasenaActual", "La contraseña actual no es correcta.");
        }
        UserValidator.validatePassword(newPassword);
        users.updatePasswordHash(actor.getId(), passwordEncoder.encode(newPassword));
    }

    private static boolean leads(Crew crew, User user) {
        return crew.getResponsible() != null && Objects.equals(crew.getResponsible().getId(), user.getId());
    }
}
