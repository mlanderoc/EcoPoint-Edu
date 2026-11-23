package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.Crew;
import com.ecopoint.model.User;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * Checks that a crew has no other open study and that none of its members
     * has one, before it takes a study (RN-098).
     *
     * @param crew        crew being assigned
     * @param openStudy   other open study of the crew, or null
     * @param busyMembers id of each busy user with the id of his other study
     * @param action      what is being done, e.g. "reabrir el estudio"
     * @throws BusinessRuleException if the crew or a member is busy
     */
    public static void validateCrewFreeForStudy(Crew crew, Long openStudy, Map<Long, Long> busyMembers, String action) {
        if (openStudy != null) {
            throw new BusinessRuleException("No se puede " + action + ": la cuadrilla " + crew.getName()
                    + " ya tiene abierto el Estudio N.º " + openStudy + ".");
        }
        requireMembersFree(crew, busyMembers, action);
    }

    private static void requireMembersFree(Crew crew, Map<Long, Long> busyMembers, String action) {
        String busy = crew.getMembers().stream()
                .filter(member -> busyMembers.containsKey(member.getId()))
                .map(member -> member.getFullName() + " (Estudio N.º " + busyMembers.get(member.getId()) + ")")
                .collect(Collectors.joining(", "));
        if (!busy.isEmpty()) {
            throw new BusinessRuleException("No se puede " + action + ": estos integrantes de la cuadrilla "
                    + crew.getName() + " ya están en otro estudio abierto: " + busy + ".");
        }
    }

    /**
     * Checks that the members added to a crew have no other open study (RN-091).
     *
     * @param crew         crew with its members
     * @param addedMembers ids of the members being added
     * @param busyMembers  id of each busy user with the id of his study
     * @throws BusinessRuleException if an added member already has an open study
     */
    public static void validateAddedMembersAreFree(Crew crew, Set<Long> addedMembers, Map<Long, Long> busyMembers) {
        for (User member : crew.getMembers()) {
            Long studyId = busyMembers.get(member.getId());
            if (addedMembers.contains(member.getId()) && studyId != null) {
                throw new BusinessRuleException(member.getFullName() + " ya está asignado al Estudio N.º " + studyId
                        + ": debe estar libre para entrar a la cuadrilla.");
            }
        }
    }
}
