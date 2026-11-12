package com.ecopoint.service;

import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.Permission;
import com.ecopoint.model.SourceType;
import com.ecopoint.model.User;
import com.ecopoint.persistence.GenerationSourceRepository;
import com.ecopoint.persistence.InstitutionRepository;
import com.ecopoint.validation.AccessPolicy;
import com.ecopoint.validation.GenerationSourceValidator;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Administration of the generation sources of an institution (RN-001 to RN-005;
 * spec 003, section 5.1; spec 004, R-321).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Service
public class GenerationSourceService {

    private final GenerationSourceRepository sources;
    private final InstitutionRepository institutions;

    /**
     * Creates the service.
     *
     * @param sources      source repository
     * @param institutions institution repository
     */
    public GenerationSourceService(GenerationSourceRepository sources, InstitutionRepository institutions) {
        this.sources = sources;
        this.institutions = institutions;
    }

    /**
     * Lists the sources of an institution.
     *
     * @param institutionId institution id
     * @param onlyActive    true to leave out inactive sources
     * @return sources ordered by code
     */
    public List<GenerationSource> list(Long institutionId, boolean onlyActive) {
        requireInstitution(institutionId);
        return sources.findByInstitution(institutionId).stream()
                .filter(source -> !onlyActive || source.isActive())
                .toList();
    }

    /**
     * Finds a source of an institution.
     *
     * @param institutionId institution id
     * @param sourceId      source id
     * @return the source
     * @throws ResourceNotFoundException if it does not exist in that institution
     */
    public GenerationSource get(Long institutionId, Long sourceId) {
        return sources.findById(sourceId)
                .filter(source -> source.getInstitutionId().equals(institutionId))
                .orElseThrow(() -> new ResourceNotFoundException("Fuente de generación", sourceId));
    }

    /**
     * Creates a source (RN-001 to RN-003).
     *
     * @param actor         user that acts
     * @param institutionId institution of the source
     * @param code          code, unique in the institution
     * @param name          name
     * @param type          type
     * @return the saved source
     */
    public GenerationSource create(User actor, Long institutionId, String code, String name, SourceType type) {
        AccessPolicy.require(actor, Permission.MANAGE_ADMINISTRATION);
        requireInstitution(institutionId);
        GenerationSource source = new GenerationSource(null, institutionId, code, name, type);
        GenerationSourceValidator.validate(source);
        GenerationSourceValidator.validateUniqueCode(source, sources.findCodesByInstitution(institutionId));
        return sources.save(source);
    }

    /**
     * Changes the code, name and type of a source.
     *
     * @param actor         user that acts
     * @param institutionId institution of the source
     * @param sourceId      source id
     * @param code          new code
     * @param name          new name
     * @param type          new type
     * @return the saved source
     */
    public GenerationSource update(User actor, Long institutionId, Long sourceId, String code, String name,
                                   SourceType type) {
        AccessPolicy.require(actor, Permission.MANAGE_ADMINISTRATION);
        GenerationSource source = get(institutionId, sourceId);
        Set<String> otherCodes = new HashSet<>(sources.findCodesByInstitution(institutionId));
        otherCodes.remove(source.getCode());
        source.setCode(code);
        source.setName(name);
        source.setType(type);
        GenerationSourceValidator.validate(source);
        GenerationSourceValidator.validateUniqueCode(source, otherCodes);
        return sources.save(source);
    }

    /**
     * Deactivates a source: it no longer appears for new records (RN-005).
     *
     * @param actor         user that acts
     * @param institutionId institution of the source
     * @param sourceId      source id
     * @return the source, now inactive
     */
    public GenerationSource deactivate(User actor, Long institutionId, Long sourceId) {
        AccessPolicy.require(actor, Permission.MANAGE_ADMINISTRATION);
        GenerationSource source = get(institutionId, sourceId);
        source.deactivate();
        return sources.save(source);
    }

    /**
     * Activates a source again.
     *
     * @param actor         user that acts
     * @param institutionId institution of the source
     * @param sourceId      source id
     * @return the source, now active
     */
    public GenerationSource activate(User actor, Long institutionId, Long sourceId) {
        AccessPolicy.require(actor, Permission.MANAGE_ADMINISTRATION);
        GenerationSource source = get(institutionId, sourceId);
        source.activate();
        return sources.save(source);
    }

    /**
     * Deletes a source that has no bags; one with bags can only be deactivated (RN-005).
     *
     * @param actor         user that acts
     * @param institutionId institution of the source
     * @param sourceId      source id
     */
    public void delete(User actor, Long institutionId, Long sourceId) {
        AccessPolicy.require(actor, Permission.MANAGE_ADMINISTRATION);
        GenerationSource source = get(institutionId, sourceId);
        GenerationSourceValidator.validateDeletion(source, sources.hasBags(sourceId));
        sources.delete(sourceId);
    }

    private void requireInstitution(Long institutionId) {
        if (institutions.findById(institutionId).isEmpty()) {
            throw new ResourceNotFoundException("Institución", institutionId);
        }
    }
}
