package com.ecopoint.persistence;

import com.ecopoint.model.WasteCategory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Storage contract for the waste subcategories catalog (spec 002, R-050).
 *
 * @author mlanderoc
 * @version 1.0
 */
public interface WasteCategoryRepository {

    /**
     * Inserts a new subcategory (id null) or updates an existing one.
     *
     * @param category subcategory to save
     * @return the same subcategory, with its id assigned
     */
    WasteCategory save(WasteCategory category);

    /**
     * Finds a subcategory by id.
     *
     * @param id subcategory id
     * @return the subcategory, or empty if it does not exist
     */
    Optional<WasteCategory> findById(Long id);

    /**
     * Lists the whole catalog ordered by group and name.
     *
     * @return subcategories, active and inactive
     */
    List<WasteCategory> findAll();

    /**
     * Returns the whole catalog indexed by id, as {@code QuarteringValidator} needs it.
     *
     * @return subcategories by id
     */
    Map<Long, WasteCategory> findAllAsMap();

    /**
     * Returns the names already used, for the uniqueness rule (RN-041).
     *
     * @return set of names
     */
    Set<String> findNames();

    /**
     * Indicates whether the subcategory already has quartering results (RN-042).
     *
     * @param categoryId subcategory id
     * @return true if it appears in at least one quartering
     */
    boolean hasResults(Long categoryId);
}
