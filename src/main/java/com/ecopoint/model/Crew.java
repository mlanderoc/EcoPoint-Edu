package com.ecopoint.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Field crew that carries out the sampling: university students led by a
 * responsible teacher (RN-085).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class Crew {

    private Long id;
    private String name;
    private User responsible;
    private final List<User> members;

    /**
     * Creates an empty crew.
     */
    public Crew() {
        this.members = new ArrayList<>();
    }

    /**
     * Creates a crew with its name and responsible teacher.
     *
     * @param id          identifier, or null if not saved yet
     * @param name        crew name, e.g. "Cuadrilla 1"
     * @param responsible teacher in charge of the crew
     */
    public Crew(Long id, String name, User responsible) {
        this();
        this.id = id;
        this.name = name;
        this.responsible = responsible;
    }

    /**
     * Indicates whether the crew is led by a user whose role may supervise
     * a crew (RN-085).
     *
     * @return true if the responsible user is a teacher
     */
    public boolean hasResponsibleTeacher() {
        return responsible != null && responsible.getRole() != null
                && responsible.getRole().canSuperviseCrew();
    }

    /**
     * Adds a member to the crew; a user already in the crew is not added twice.
     *
     * @param user member to add
     * @return true if the user was added, false if it was null or already a member
     */
    public boolean addMember(User user) {
        if (user == null || isMember(user)) {
            return false;
        }
        return members.add(user);
    }

    /**
     * Removes a member from the crew.
     *
     * @param user member to remove
     * @return true if the user was a member
     */
    public boolean removeMember(User user) {
        return user != null && members.removeIf(member -> sameUser(member, user));
    }

    /**
     * Indicates whether a user belongs to the crew, comparing by identifier
     * when both have one.
     *
     * @param user user to look for
     * @return true if the user is a member
     */
    public boolean isMember(User user) {
        return user != null && members.stream().anyMatch(member -> sameUser(member, user));
    }

    private static boolean sameUser(User first, User second) {
        if (first.getId() != null && second.getId() != null) {
            return Objects.equals(first.getId(), second.getId());
        }
        return first == second;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getResponsible() {
        return responsible;
    }

    public void setResponsible(User responsible) {
        this.responsible = responsible;
    }

    /**
     * Returns the members of the crew.
     *
     * @return unmodifiable list of members
     */
    public List<User> getMembers() {
        return Collections.unmodifiableList(members);
    }
}
