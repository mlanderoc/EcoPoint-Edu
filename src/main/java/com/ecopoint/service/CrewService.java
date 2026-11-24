package com.ecopoint.service;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.Crew;
import com.ecopoint.model.Permission;
import com.ecopoint.model.User;
import com.ecopoint.persistence.CrewRepository;
import com.ecopoint.persistence.StudyRepository;
import com.ecopoint.persistence.UserRepository;
import com.ecopoint.validation.AccessPolicy;
import com.ecopoint.validation.CrewValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Field crews with their responsible teacher and members (RN-085; spec 003,
 * section 5.1; spec 004, R-323). A crew and its members are saved together.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Service
public class CrewService {

    private final CrewRepository crews;
    private final UserRepository users;
    private final StudyRepository studies;

    /**
     * Creates the service.
     *
     * @param crews crew repository
     * @param users   user repository
     * @param studies study repository, to know who has an open study (RN-091)
     */
    public CrewService(CrewRepository crews, UserRepository users, StudyRepository studies) {
        this.crews = crews;
        this.users = users;
        this.studies = studies;
    }

    /**
     * Lists the users that cannot join a crew because they have another open
     * study (RN-091).
     *
     * @param actor  teacher that asks
     * @param crewId crew being edited; null for a new crew
     * @return id of each busy user with the id of his study
     */
    public Map<Long, Long> busyMembers(User actor, Long crewId) {
        AccessPolicy.require(actor, Permission.MANAGE_ADMINISTRATION);
        return studies.findMembersInOpenStudies(crewId);
    }

    /**
     * Lists the crews of the institution of the actor.
     *
     * @param actor user that asks
     * @return crews of the institution
     */
    public List<Crew> list(User actor) {
        return crews.findByInstitution(actor.getInstitutionId());
    }

    /**
     * Lists the crews that cannot take a study because they, or one of their
     * members, are in another open study (RN-098).
     *
     * @param actor   teacher that asks
     * @param studyId study being edited; null for a new one
     * @return id of each unavailable crew with the id of the study that holds it
     */
    public Map<Long, Long> unavailableCrews(User actor, Long studyId) {
        AccessPolicy.require(actor, Permission.MANAGE_ADMINISTRATION);
        Map<Long, Long> ofCrews = studies.findCrewsWithOpenStudiesOtherThan(studyId);
        Map<Long, Long> ofMembers = studies.findMembersInOpenStudiesOtherThan(studyId);
        Map<Long, Long> unavailable = new LinkedHashMap<>();
        for (Crew crew : list(actor)) {
            Long holder = ofCrews.get(crew.getId());
            if (holder == null) {
                holder = crew.getMembers().stream().map(member -> ofMembers.get(member.getId()))
                        .filter(Objects::nonNull).findFirst().orElse(null);
            }
            if (holder != null) {
                unavailable.put(crew.getId(), holder);
            }
        }
        return unavailable;
    }

    /**
     * Finds a crew.
     *
     * @param id crew id
     * @return the crew
     */
    public Crew get(Long id) {
        return crews.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cuadrilla", id));
    }

    /**
     * Finds a crew the user may see: a teacher of its institution or one of
     * its members (RN-097).
     *
     * @param actor user that asks
     * @param id    crew id
     * @return the crew
     */
    public Crew getFor(User actor, Long id) {
        Crew crew = get(id);
        if (!AccessPolicy.can(actor, Permission.MANAGE_ADMINISTRATION)) {
            if (!crew.isMember(actor)) {
                throw new AccessDeniedException("Solo los integrantes de la cuadrilla pueden verla.");
            }
        } else if (crew.getResponsible() != null) {
            AccessPolicy.requireSameInstitution(actor, crew.getResponsible());
        }
        return crew;
    }

    /**
     * Creates a crew.
     *
     * @param actor         teacher that acts
     * @param name          crew name
     * @param responsibleId id of the responsible teacher (RN-085)
     * @param memberIds     ids of the members
     * @return the saved crew
     */
    @Transactional
    public Crew create(User actor, String name, Long responsibleId, List<Long> memberIds) {
        AccessPolicy.require(actor, Permission.MANAGE_ADMINISTRATION);
        Crew crew = new Crew(null, name, findUser(responsibleId));
        addMembers(crew, memberIds);
        CrewValidator.validate(crew);
        CrewValidator.validateAddedMembersAreFree(crew, idsOf(crew), studies.findMembersInOpenStudies(null));
        return crews.save(crew);
    }

    /**
     * Replaces the name, the responsible teacher and the members of a crew.
     *
     * @param actor         teacher that acts
     * @param id            crew id
     * @param name          new name
     * @param responsibleId id of the responsible teacher
     * @param memberIds     ids of the members
     * @return the saved crew
     */
    @Transactional
    public Crew update(User actor, Long id, String name, Long responsibleId, List<Long> memberIds) {
        AccessPolicy.require(actor, Permission.MANAGE_ADMINISTRATION);
        Crew current = get(id);
        Crew crew = new Crew(id, name, findUser(responsibleId));
        addMembers(crew, memberIds);
        CrewValidator.validate(crew);
        Set<Long> added = idsOf(crew);
        added.removeAll(idsOf(current));
        CrewValidator.validateAddedMembersAreFree(crew, added, studies.findMembersInOpenStudies(id));
        return crews.save(crew);
    }

    private Set<Long> idsOf(Crew crew) {
        Set<Long> ids = new HashSet<>();
        crew.getMembers().forEach(member -> ids.add(member.getId()));
        return ids;
    }

    private void addMembers(Crew crew, List<Long> memberIds) {
        if (memberIds != null) {
            memberIds.stream().distinct().map(this::findUser).forEach(crew::addMember);
        }
    }

    private User findUser(Long id) {
        if (id == null) {
            return null;
        }
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }
}
