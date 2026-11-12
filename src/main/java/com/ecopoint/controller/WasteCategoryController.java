package com.ecopoint.controller;

import com.ecopoint.dto.WasteCategoryRequest;
import com.ecopoint.dto.WasteCategoryResponse;
import com.ecopoint.model.WasteGroup;
import com.ecopoint.security.EcoPointUserDetails;
import com.ecopoint.service.WasteCategoryService;
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
 * Waste subcategories of the four groups of Res. 2184 of 2019 (spec 003, section 5.1).
 *
 * @author craulgomez
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/waste-categories")
public class WasteCategoryController implements FieldNameMapping {

    private static final Map<String, String> FIELDS = Map.of(
            "nombre", "name", "grupo", "group", "descripcion", "description");

    private final WasteCategoryService categories;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param categories  subcategory service
     * @param currentUser user of the session
     */
    public WasteCategoryController(WasteCategoryService categories, CurrentUser currentUser) {
        this.categories = categories;
        this.currentUser = currentUser;
    }

    @Override
    public Map<String, String> fieldNames() {
        return FIELDS;
    }

    /**
     * Lists the subcategories, optionally of one group.
     *
     * @param group group to keep, or none
     * @return subcategories
     */
    @GetMapping
    public List<WasteCategoryResponse> list(@RequestParam(required = false) WasteGroup group) {
        return categories.list(group).stream().map(WasteCategoryResponse::from).toList();
    }

    /**
     * Creates a subcategory.
     *
     * @param principal user of the session
     * @param request   data of the subcategory
     * @return the created subcategory
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WasteCategoryResponse create(@AuthenticationPrincipal EcoPointUserDetails principal,
                                        @RequestBody WasteCategoryRequest request) {
        return WasteCategoryResponse.from(categories.create(currentUser.of(principal), request.toModel()));
    }

    /**
     * Edits a subcategory.
     *
     * @param principal user of the session
     * @param id        subcategory id
     * @param request   new data
     * @return the saved subcategory
     */
    @PutMapping("/{id}")
    public WasteCategoryResponse update(@AuthenticationPrincipal EcoPointUserDetails principal, @PathVariable Long id,
                                        @RequestBody WasteCategoryRequest request) {
        return WasteCategoryResponse.from(categories.update(currentUser.of(principal), id, request.toModel()));
    }
}
