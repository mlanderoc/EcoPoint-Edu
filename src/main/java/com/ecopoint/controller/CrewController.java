package com.ecopoint.controller;

import com.ecopoint.dto.CrewRequest;
import com.ecopoint.dto.CrewResponse;
import com.ecopoint.security.EcoPointUserDetails;
import com.ecopoint.service.CrewService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Field crews (spec 003, section 5.1).
 *
 * @author craulgomez
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/crews")
public class CrewController implements FieldNameMapping {

    private static final Map<String, String> FIELDS = Map.of("nombre", "name");

    private final CrewService crews;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param crews       crew service
     * @param currentUser user of the session
     */
    public CrewController(CrewService crews, CurrentUser currentUser) {
        this.crews = crews;
        this.currentUser = currentUser;
    }

    @Override
    public Map<String, String> fieldNames() {
        return FIELDS;
    }

    /**
     * Lists the crews of the institution of the user.
     *
     * @param principal user of the session
     * @return crews
     */
    @GetMapping
    public List<CrewResponse> list(@AuthenticationPrincipal EcoPointUserDetails principal) {
        return crews.list(currentUser.of(principal)).stream().map(CrewResponse::from).toList();
    }

    /**
     * Shows one crew.
     *
     * @param id crew id
     * @return the crew
     */
    @GetMapping("/{id}")
    public CrewResponse get(@PathVariable Long id) {
        return CrewResponse.from(crews.get(id));
    }

    /**
     * Creates a crew with its members.
     *
     * @param principal user of the session
     * @param request   data of the crew
     * @return the created crew
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CrewResponse create(@AuthenticationPrincipal EcoPointUserDetails principal,
                               @RequestBody CrewRequest request) {
        return CrewResponse.from(crews.create(currentUser.of(principal), request.name(), request.responsibleId(),
                request.memberIds()));
    }

    /**
     * Replaces the data and the members of a crew.
     *
     * @param principal user of the session
     * @param id        crew id
     * @param request   new data
     * @return the saved crew
     */
    @PutMapping("/{id}")
    public CrewResponse update(@AuthenticationPrincipal EcoPointUserDetails principal, @PathVariable Long id,
                               @RequestBody CrewRequest request) {
        return CrewResponse.from(crews.update(currentUser.of(principal), id, request.name(), request.responsibleId(),
                request.memberIds()));
    }
}
