package com.ecopoint.dto;

import com.ecopoint.model.Crew;
import com.ecopoint.model.User;

import java.util.List;

/**
 * Crew as the API returns it.
 *
 * @param id              crew id
 * @param name            name
 * @param responsibleId   id of the responsible teacher
 * @param responsibleName full name of the responsible teacher
 * @param memberIds       ids of the members
 * @author craulgomez
 * @version 1.0
 */
public record CrewResponse(Long id, String name, Long responsibleId, String responsibleName, List<Long> memberIds) {

    /**
     * Copies a crew of the domain.
     *
     * @param crew domain crew
     * @return the response
     */
    public static CrewResponse from(Crew crew) {
        User responsible = crew.getResponsible();
        return new CrewResponse(crew.getId(), crew.getName(), responsible == null ? null : responsible.getId(),
                responsible == null ? null : responsible.getFullName(),
                crew.getMembers().stream().map(User::getId).toList());
    }
}
