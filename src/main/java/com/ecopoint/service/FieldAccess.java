package com.ecopoint.service;

import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.Crew;
import com.ecopoint.model.Permission;
import com.ecopoint.model.User;
import com.ecopoint.persistence.CrewRepository;
import com.ecopoint.validation.AccessPolicy;
import org.springframework.stereotype.Component;

/**
 * Who may register field data in a study: a teacher in any study, a student
 * only in the studies of a crew he belongs to (RN-089, D14). The field
 * services call it before registering population, bags, quarterings and
 * densities.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Component
public class FieldAccess {

    private final CrewRepository crews;

    /**
     * Creates the component.
     *
     * @param crews crew repository
     */
    public FieldAccess(CrewRepository crews) {
        this.crews = crews;
    }

    /**
     * Requires that the user may register data in the study.
     *
     * @param actor user that registers
     * @param study study of the shift
     * @throws com.ecopoint.exceptions.AccessDeniedException if a student is not in the crew of the study
     */
    public void require(User actor, CharacterizationStudy study) {
        AccessPolicy.requireCrewMember(actor, crewOf(study));
    }

    /**
     * Indicates whether the user may register data in the study, without throwing.
     *
     * @param actor user to check
     * @param study study to check
     * @return true for a teacher, or for a student of the crew of the study
     */
    public boolean allows(User actor, CharacterizationStudy study) {
        if (AccessPolicy.can(actor, Permission.MANAGE_STUDIES)) {
            return true;
        }
        Crew crew = crewOf(study);
        return crew != null && crew.isMember(actor);
    }

    private Crew crewOf(CharacterizationStudy study) {
        return study.getCrewId() == null ? null : crews.findById(study.getCrewId()).orElse(null);
    }
}
