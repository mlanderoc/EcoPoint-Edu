package com.ecopoint.controller;

import com.ecopoint.dto.SourceRequest;
import com.ecopoint.dto.SourceResponse;
import com.ecopoint.security.EcoPointUserDetails;
import com.ecopoint.service.GenerationSourceService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Generation sources of an institution (spec 003, section 5.1).
 *
 * @author craulgomez
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/institutions/{institutionId}/sources")
public class GenerationSourceController implements FieldNameMapping {

    private static final Map<String, String> FIELDS = Map.of(
            "codigo", "code", "nombre", "name", "tipo", "type");

    private final GenerationSourceService sources;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param sources     source service
     * @param currentUser user of the session
     */
    public GenerationSourceController(GenerationSourceService sources, CurrentUser currentUser) {
        this.sources = sources;
        this.currentUser = currentUser;
    }

    @Override
    public Map<String, String> fieldNames() {
        return FIELDS;
    }

    /**
     * Lists the sources of the institution.
     *
     * @param institutionId institution id
     * @param active        true to leave out inactive sources
     * @return sources
     */
    @GetMapping
    public List<SourceResponse> list(@PathVariable Long institutionId,
                                     @RequestParam(required = false) Boolean active) {
        return sources.list(institutionId, active).stream().map(SourceResponse::from).toList();
    }

    /**
     * Shows one source.
     *
     * @param institutionId institution id
     * @param sourceId      source id
     * @return the source
     */
    @GetMapping("/{sourceId}")
    public SourceResponse get(@PathVariable Long institutionId, @PathVariable Long sourceId) {
        return SourceResponse.from(sources.get(institutionId, sourceId));
    }

    /**
     * Creates a source.
     *
     * @param principal     user of the session
     * @param institutionId institution id
     * @param request       data of the source
     * @return the created source
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SourceResponse create(@AuthenticationPrincipal EcoPointUserDetails principal,
                                 @PathVariable Long institutionId, @RequestBody SourceRequest request) {
        return SourceResponse.from(sources.create(currentUser.of(principal), institutionId, request.code(),
                request.name(), request.type(), request.createActive()));
    }

    /**
     * Edits a source.
     *
     * @param principal     user of the session
     * @param institutionId institution id
     * @param sourceId      source id
     * @param request       new data
     * @return the saved source
     */
    @PutMapping("/{sourceId}")
    public SourceResponse update(@AuthenticationPrincipal EcoPointUserDetails principal,
                                 @PathVariable Long institutionId, @PathVariable Long sourceId,
                                 @RequestBody SourceRequest request) {
        return SourceResponse.from(sources.update(currentUser.of(principal), institutionId, sourceId,
                request.code(), request.name(), request.type()));
    }

    /**
     * Deactivates a source (RN-005).
     *
     * @param principal     user of the session
     * @param institutionId institution id
     * @param sourceId      source id
     * @return the source, now inactive
     */
    @PostMapping("/{sourceId}/deactivate")
    public SourceResponse deactivate(@AuthenticationPrincipal EcoPointUserDetails principal,
                                     @PathVariable Long institutionId, @PathVariable Long sourceId) {
        return SourceResponse.from(sources.deactivate(currentUser.of(principal), institutionId, sourceId));
    }

    /**
     * Activates a source again.
     *
     * @param principal     user of the session
     * @param institutionId institution id
     * @param sourceId      source id
     * @return the source, now active
     */
    @PostMapping("/{sourceId}/activate")
    public SourceResponse activate(@AuthenticationPrincipal EcoPointUserDetails principal,
                                   @PathVariable Long institutionId, @PathVariable Long sourceId) {
        return SourceResponse.from(sources.activate(currentUser.of(principal), institutionId, sourceId));
    }

    /**
     * Deletes a source without bags (RN-005).
     *
     * @param principal     user of the session
     * @param institutionId institution id
     * @param sourceId      source id
     */
    @DeleteMapping("/{sourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal EcoPointUserDetails principal, @PathVariable Long institutionId,
                       @PathVariable Long sourceId) {
        sources.delete(currentUser.of(principal), institutionId, sourceId);
    }
}
