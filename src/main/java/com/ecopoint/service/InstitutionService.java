package com.ecopoint.service;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.Institution;
import com.ecopoint.model.Permission;
import com.ecopoint.model.User;
import com.ecopoint.persistence.InstitutionRepository;
import com.ecopoint.validation.AccessPolicy;
import com.ecopoint.validation.InstitutionValidator;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Administration of institutions (spec 003, section 5.1; spec 004, R-320).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Service
public class InstitutionService {

    private final InstitutionRepository institutions;

    /**
     * Creates the service.
     *
     * @param institutions institution repository
     */
    public InstitutionService(InstitutionRepository institutions) {
        this.institutions = institutions;
    }

    /**
     * Lists all institutions.
     *
     * @return institutions ordered by name
     */
    public List<Institution> list() {
        return institutions.findAll();
    }

    /**
     * Finds an institution.
     *
     * @param id institution id
     * @return the institution
     * @throws ResourceNotFoundException if it does not exist
     */
    public Institution get(Long id) {
        return institutions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Institución", id));
    }

    /**
     * Creates an institution.
     *
     * @param actor       user that acts
     * @param institution data of the new institution
     * @return the saved institution
     */
    public Institution create(User actor, Institution institution) {
        AccessPolicy.require(actor, Permission.MANAGE_ADMINISTRATION);
        institution.setId(null);
        InstitutionValidator.validate(institution);
        requireFreeName(institution.getName());
        return institutions.save(institution);
    }

    /**
     * Updates the data, the map view and the characterization settings of an
     * institution (spec 007, R-623).
     *
     * @param actor   user that acts
     * @param id      institution id
     * @param changes new data
     * @return the saved institution
     */
    public Institution update(User actor, Long id, Institution changes) {
        AccessPolicy.require(actor, Permission.MANAGE_ADMINISTRATION);
        Institution current = get(id);
        boolean renamed = changes.getName() != null && !changes.getName().trim().equalsIgnoreCase(current.getName());
        current.setName(changes.getName());
        current.setAddress(changes.getAddress());
        current.setMapCenter(changes.getMapCenter());
        current.setMapZoom(changes.getMapZoom());
        current.setSettings(changes.getSettings());
        InstitutionValidator.validate(current);
        if (renamed) {
            requireFreeName(current.getName());
        }
        return institutions.save(current);
    }

    private void requireFreeName(String name) {
        if (institutions.existsByName(name.trim())) {
            throw new BusinessRuleException("Ya existe una institución con el nombre " + name.trim() + ".");
        }
    }
}
