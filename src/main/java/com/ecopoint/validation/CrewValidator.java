package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.Crew;
import com.ecopoint.model.User;

/**
 * Stateless validator for field crews (RN-085), applying GRASP Pure
 * Fabrication and SOLID SRP.
 *
 * @author craulgomez
 * @version 1.0
 */
public final class CrewValidator {

    private static final int MAX_NAME_LENGTH = 80;

    private CrewValidator() {
    }

    /**
     * Validates a crew: it needs a name, an active responsible teacher and
     * only active members.
     *
     * @param crew crew to validate
     * @throws InvalidDataException  if the crew or its name is missing
     * @throws BusinessRuleException if there is no responsible teacher or a member is inactive
     */
    public static void validate(Crew crew) {
        if (crew == null) {
            throw new InvalidDataException("cuadrilla", "La cuadrilla no puede ser nula.");
        }
        if (crew.getName() == null || crew.getName().isBlank()) {
            throw new InvalidDataException("nombre", "El nombre de la cuadrilla es obligatorio.");
        }
        if (crew.getName().trim().length() > MAX_NAME_LENGTH) {
            throw new InvalidDataException("nombre",
                    "El nombre de la cuadrilla no puede superar " + MAX_NAME_LENGTH + " caracteres.");
        }
        if (!crew.hasResponsibleTeacher()) {
            throw new BusinessRuleException("La cuadrilla " + crew.getName()
                    + " debe tener un docente como responsable.");
        }
        if (!crew.getResponsible().isActive()) {
            throw new BusinessRuleException("El docente responsable de la cuadrilla " + crew.getName()
                    + " está inactivo.");
        }
        for (User member : crew.getMembers()) {
            if (!member.isActive()) {
                throw new BusinessRuleException("El integrante " + member.getFullName()
                        + " está inactivo y no puede estar en la cuadrilla.");
            }
        }
    }
}
