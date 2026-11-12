package com.ecopoint.service;

import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.Crew;
import com.ecopoint.model.Permission;
import com.ecopoint.model.User;
import com.ecopoint.persistence.CrewRepository;
import com.ecopoint.persistence.UserRepository;
import com.ecopoint.validation.AccessPolicy;
import com.ecopoint.validation.CrewValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    /**
     * Creates the service.
     *
     * @param crews crew repository
     * @param users user repository
     */
    public CrewService(CrewRepository crews, UserRepository users) {
        this.crews = crews;
        this.users = users;
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
     * Finds a crew.
     *
     * @param id crew id
     * @return the crew
     */
    public Crew get(Long id) {
        return crews.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cuadrilla", id));
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
        get(id);
        Crew crew = new Crew(id, name, findUser(responsibleId));
        addMembers(crew, memberIds);
        CrewValidator.validate(crew);
        return crews.save(crew);
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
