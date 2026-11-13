package com.ecopoint.controller;

import com.ecopoint.dto.InstitutionRequest;
import com.ecopoint.dto.InstitutionResponse;
import com.ecopoint.security.EcoPointUserDetails;
import com.ecopoint.service.InstitutionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * Institutions (spec 003, section 5.1).
 *
 * @author craulgomez
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/institutions")
public class InstitutionController implements FieldNameMapping {

    private static final Map<String, String> FIELDS = Map.of(
            "nombre", "name", "direccion", "address", "zoom", "mapZoom");

    private final InstitutionService institutions;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param institutions institution service
     * @param currentUser  user of the session
     */
    public InstitutionController(InstitutionService institutions, CurrentUser currentUser) {
        this.institutions = institutions;
        this.currentUser = currentUser;
    }

    @Override
    public Map<String, String> fieldNames() {
        return FIELDS;
    }

    /**
     * Lists the institutions.
     *
     * @return institutions
     * @param active true for the active ones, false for the inactive ones, none for all (R-325)
     */
    @GetMapping
    public List<InstitutionResponse> list(@RequestParam(required = false) Boolean active) {
        return institutions.list(active).stream().map(InstitutionResponse::from).toList();
    }

    /**
     * Shows one institution.
     *
     * @param id institution id
     * @return the institution
     */
    @GetMapping("/{id}")
    public InstitutionResponse get(@PathVariable Long id) {
        return InstitutionResponse.from(institutions.get(id));
    }

    /**
     * Creates an institution.
     *
     * @param principal user of the session
     * @param request   data of the institution
     * @return the created institution
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstitutionResponse create(@AuthenticationPrincipal EcoPointUserDetails principal,
                                      @RequestBody InstitutionRequest request) {
        return InstitutionResponse.from(institutions.create(currentUser.of(principal), request.toModel()));
    }

    /**
     * Edits an institution, including its map view.
     *
     * @param principal user of the session
     * @param id        institution id
     * @param request   new data
     * @return the saved institution
     */
    @PutMapping("/{id}")
    public InstitutionResponse update(@AuthenticationPrincipal EcoPointUserDetails principal, @PathVariable Long id,
                                      @RequestBody InstitutionRequest request) {
        return InstitutionResponse.from(institutions.update(currentUser.of(principal), id, request.toModel()));
    }

    /**
     * Activates an institution again (RN-006).
     *
     * @param principal user of the session
     * @param id        institution id
     * @return the institution, now active
     */
    @PostMapping("/{id}/activate")
    public InstitutionResponse activate(@AuthenticationPrincipal EcoPointUserDetails principal, @PathVariable Long id) {
        return InstitutionResponse.from(institutions.activate(currentUser.of(principal), id));
    }

    /**
     * Deactivates an institution: it keeps its data but admits no new studies (RN-006).
     *
     * @param principal user of the session
     * @param id        institution id
     * @return the institution, now inactive
     */
    @PostMapping("/{id}/deactivate")
    public InstitutionResponse deactivate(@AuthenticationPrincipal EcoPointUserDetails principal,
                                          @PathVariable Long id) {
        return InstitutionResponse.from(institutions.deactivate(currentUser.of(principal), id));
    }
}
